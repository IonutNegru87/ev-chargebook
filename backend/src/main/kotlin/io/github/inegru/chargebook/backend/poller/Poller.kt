package io.github.inegru.chargebook.backend.poller

import io.github.inegru.chargebook.backend.auth.AuthRequiredException
import io.github.inegru.chargebook.backend.persistence.SnapshotLocalDataSource
import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import io.github.inegru.chargebook.shared.data.VolvoVehiclesDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.result.Result
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
 * by [PollingScheduler] and persists each snapshot via [snapshots].
 *
 * Owns no auth state — when [AuthRequiredException] surfaces, it parks for
 * [authRetryInterval] and retries. The user signs in via `/auth/start`, the
 * next tick succeeds, polling resumes.
 *
 * Session detection / linkage will be added in milestone 3; for now snapshots
 * are persisted with `sessionId = null`.
 */
class Poller(
    private val vehicles: VolvoVehiclesDataSource,
    private val energy: VolvoEnergyDataSource,
    private val snapshots: SnapshotLocalDataSource,
    private val authRetryInterval: Duration = 60.seconds,
    private val networkRetryInterval: Duration = 60.seconds,
) {
    private val log = LoggerFactory.getLogger("Poller")

    fun start(scope: CoroutineScope): Job = scope.launch { runLoop() }

    private suspend fun runLoop() {
        log.info("Poller started")
        var vin: String? = null
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
                val next = pollOnce(vin)
                delay(next)
            } catch (e: AuthRequiredException) {
                log.info("Not authenticated; sleeping ${authRetryInterval} (sign in at /auth/start)")
                vin = null
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

    /** Returns the duration to wait before the next poll. */
    private suspend fun pollOnce(vin: String): Duration = when (val r = energy.rechargeStatus(vin)) {
        is Result.Success -> {
            persist(r.data)
            PollingScheduler.intervalFor(r.data.connectionStatus, r.data.chargingStatus).also {
                log.info(
                    "Polled $vin — SoC=${r.data.socPct}%, status=${r.data.chargingStatus}, " +
                        "connection=${r.data.connectionStatus}, power=${r.data.powerKw}kW; next in $it",
                )
            }
        }
        is Result.Error -> {
            log.warn("Poll failed for $vin: ${r.error}; sleeping $networkRetryInterval")
            // 401 means token was revoked or scope changed — force re-auth path
            if (r.error == DataError.Network.UNAUTHORIZED) {
                throw AuthRequiredException("Upstream returned 401")
            }
            networkRetryInterval
        }
    }

    private suspend fun persist(snapshot: ChargingSnapshot) {
        when (val r = snapshots.insert(snapshot)) {
            is Result.Error -> log.error("Failed to persist snapshot: ${r.error}")
            is Result.Success -> Unit
        }
    }

}
