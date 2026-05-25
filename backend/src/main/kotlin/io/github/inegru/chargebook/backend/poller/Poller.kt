package io.github.inegru.chargebook.backend.poller

import io.github.inegru.chargebook.backend.auth.AuthRequiredException
import io.github.inegru.chargebook.backend.persistence.SessionLocalDataSource
import io.github.inegru.chargebook.backend.persistence.SnapshotLocalDataSource
import io.github.inegru.chargebook.shared.analytics.SessionAggregates
import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import io.github.inegru.chargebook.shared.data.VolvoVehiclesDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ConnectionType
import io.github.inegru.chargebook.shared.result.Result
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Long-running coroutine that polls the Volvo Energy API at the cadence chosen
 * by [PollingScheduler], runs each snapshot through [sessionDetector], and
 * persists snapshots + sessions.
 *
 * Owns no auth state — when [AuthRequiredException] surfaces, it parks for
 * [authRetryInterval] and retries. The user signs in via `/auth/start`, the
 * next tick succeeds, polling resumes.
 *
 * Restart handling: on first run the poller asks the DB for any open session
 * (`ended_at IS NULL`) for the tracked VIN; if one exists, the detector is
 * primed in CHARGING so we continue that session instead of opening a new one.
 */
class Poller(
    private val vehicles: VolvoVehiclesDataSource,
    private val energy: VolvoEnergyDataSource,
    private val snapshots: SnapshotLocalDataSource,
    private val sessions: SessionLocalDataSource,
    private val sessionDetector: SessionDetector,
    private val snapshotBus: SnapshotBus,
    private val authRetryInterval: Duration = 60.seconds,
    private val networkRetryInterval: Duration = 60.seconds,
) {
    private val log = LoggerFactory.getLogger("Poller")

    fun start(scope: CoroutineScope): Job = scope.launch { runLoop() }

    private suspend fun runLoop() {
        log.info("Poller started")
        var vin: String? = null
        var resumed = false
        while (currentCoroutineContext().isActive) {
            try {
                if (vin == null) {
                    vin = resolveVin()
                    if (vin == null) {
                        delay(authRetryInterval)
                        continue
                    }
                    log.info("Tracking VIN $vin")
                }
                if (!resumed) {
                    primeDetectorFromOpenSession(vin)
                    resumed = true
                }
                val next = pollOnce(vin)
                delay(next)
            } catch (e: AuthRequiredException) {
                log.info("Not authenticated; sleeping $authRetryInterval (sign in at /auth/start)")
                vin = null
                resumed = false
                sessionDetector.reset()
                delay(authRetryInterval)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Unexpected polling error; sleeping $networkRetryInterval", e)
                delay(networkRetryInterval)
            }
        }
        log.info("Poller stopped")
    }

    private suspend fun resolveVin(): String? = when (val r = vehicles.list()) {
        is Result.Success -> r.data.firstOrNull()
            ?: run {
                log.warn("No vehicles on this account")
                null
            }
        is Result.Error -> {
            log.warn("Failed to list vehicles: ${r.error}")
            null
        }
    }

    private suspend fun primeDetectorFromOpenSession(vin: String) {
        when (val r = sessions.openSessionFor(vin)) {
            is Result.Success -> {
                val open = r.data
                if (open != null) {
                    log.info("Resuming open session ${open.id} (started ${open.startedAt})")
                    sessionDetector.resumeCharging()
                }
            }
            is Result.Error -> log.warn("Could not check for open sessions: ${r.error}")
        }
    }

    /** Polls one snapshot, drives session detection, persists, returns wait until next poll. */
    private suspend fun pollOnce(vin: String): Duration = when (val r = energy.rechargeStatus(vin)) {
        is Result.Success -> {
            val tagged = handleSessionLifecycle(vin, r.data)
            persist(tagged)
            PollingScheduler.intervalFor(tagged.connectionStatus, tagged.chargingStatus).also {
                log.info(
                    "Polled $vin — SoC=${tagged.socPct}%, status=${tagged.chargingStatus}, " +
                        "connection=${tagged.connectionStatus}, power=${tagged.powerKw}kW, " +
                        "session=${tagged.sessionId ?: "none"}; next in $it",
                )
            }
        }
        is Result.Error -> {
            log.warn("Poll failed for $vin: ${r.error}; sleeping $networkRetryInterval")
            if (r.error == DataError.Network.UNAUTHORIZED) {
                throw AuthRequiredException("Upstream returned 401")
            }
            networkRetryInterval
        }
    }

    /**
     * Runs [snapshot] through the detector, handles Start/End events, and
     * returns a copy of the snapshot with `sessionId` set to the open session
     * (if any). The snapshot returned is what we'll persist.
     */
    private suspend fun handleSessionLifecycle(
        vin: String,
        snapshot: ChargingSnapshot,
    ): ChargingSnapshot {
        return when (val event = sessionDetector.onSnapshot(snapshot)) {
            is SessionDetector.SessionEvent.Start -> {
                // Idempotent: if a session is already open (e.g. detector was
                // resumed after restart), reuse it instead of opening a duplicate.
                val open = openSessionOrNull(vin)
                val session = open ?: openNewSession(vin, snapshot)
                snapshot.copy(sessionId = session.id)
            }
            is SessionDetector.SessionEvent.End -> {
                val open = openSessionOrNull(vin)
                if (open != null) {
                    closeSession(open, endSnapshot = snapshot)
                    snapshot.copy(sessionId = open.id)
                } else {
                    snapshot
                }
            }
            null -> {
                val open = openSessionOrNull(vin)
                snapshot.copy(sessionId = open?.id)
            }
        }
    }

    private suspend fun openSessionOrNull(vin: String): ChargingSession? =
        when (val r = sessions.openSessionFor(vin)) {
            is Result.Success -> r.data
            is Result.Error -> {
                log.warn("openSessionFor($vin) failed: ${r.error}")
                null
            }
        }

    private suspend fun openNewSession(vin: String, snapshot: ChargingSnapshot): ChargingSession {
        val session = ChargingSession(
            id = UUID.randomUUID().toString(),
            vehicleVin = vin,
            startedAt = snapshot.recordedAt,
            startSocPct = snapshot.socPct,
            connectionType = snapshot.connectionStatus.toConnectionType(),
        )
        when (val r = sessions.insert(session)) {
            is Result.Error -> log.error("Failed to open new session: ${r.error}")
            is Result.Success -> log.info("Opened session ${session.id} for $vin at ${session.startedAt}")
        }
        return session
    }

    private suspend fun closeSession(
        open: ChargingSession,
        endSnapshot: ChargingSnapshot,
    ) {
        // Pull all snapshots already persisted for this session, append the
        // ending one (not yet persisted), and aggregate.
        val priorSnapshots = when (val r = snapshots.forSession(open.id)) {
            is Result.Success -> r.data
            is Result.Error -> {
                log.warn("Could not read snapshots for session ${open.id}: ${r.error}")
                emptyList()
            }
        }
        val all = priorSnapshots + endSnapshot.copy(sessionId = open.id)
        val agg = SessionAggregates.compute(all)
        val closed = open.copy(
            endedAt = endSnapshot.recordedAt,
            startSocPct = open.startSocPct ?: agg.startSocPct,
            endSocPct = endSnapshot.socPct ?: agg.endSocPct,
            energyKwh = agg.energyKwh.takeIf { it > 0.0 },
            avgPowerKw = agg.avgPowerKw.takeIf { it > 0.0 },
            peakPowerKw = agg.peakPowerKw.takeIf { it > 0.0 },
        )
        when (val r = sessions.update(closed)) {
            is Result.Error -> log.error("Failed to close session ${open.id}: ${r.error}")
            is Result.Success -> log.info(
                "Closed session ${open.id}: ${closed.startSocPct}% → ${closed.endSocPct}%, " +
                    "${"%.2f".format(closed.energyKwh ?: 0.0)} kWh",
            )
        }
    }

    private suspend fun persist(snapshot: ChargingSnapshot) {
        when (val r = snapshots.insert(snapshot)) {
            is Result.Error -> log.error("Failed to persist snapshot: ${r.error}")
            is Result.Success -> snapshotBus.publish(snapshot)
        }
    }
}

private fun ChargingConnectionStatus.toConnectionType(): ConnectionType = when (this) {
    ChargingConnectionStatus.CONNECTED_AC -> ConnectionType.AC
    ChargingConnectionStatus.CONNECTED_DC -> ConnectionType.DC
    else -> ConnectionType.UNKNOWN
}
