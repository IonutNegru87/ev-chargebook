package io.github.inegru.chargebook.backend.poller

import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ChargingSystemStatus

/**
 * State machine that turns the polling stream into [SessionEvent]s.
 *
 * ```
 * DISCONNECTED --(CONNECTED)-->         IDLE_PLUGGED
 * IDLE_PLUGGED --(CHARGING)-->          CHARGING       [SessionStart]
 * CHARGING     --(¬CHARGING × N)-->     IDLE_PLUGGED   [SessionEnd]
 * ANY          --(DISCONNECTED)-->      DISCONNECTED   [SessionEnd if was CHARGING]
 * ```
 *
 * `N` (debounce) is [nonChargingPollsToEnd] and exists because the EX30
 * intermittently reports `IDLE` mid-session.
 *
 * State is held in-process — survives a single Poller's lifetime, not a server
 * restart. The Poller is the only caller (single coroutine), so no locking is
 * needed.
 */
class SessionDetector(
    private val nonChargingPollsToEnd: Int = 3,
) {
    sealed interface SessionEvent {
        data class Start(val snapshot: ChargingSnapshot) : SessionEvent
        data class End(val snapshot: ChargingSnapshot) : SessionEvent
    }

    private enum class State { DISCONNECTED, IDLE_PLUGGED, CHARGING }

    private var state: State = State.DISCONNECTED
    private var nonChargingStreak: Int = 0

    fun onSnapshot(snapshot: ChargingSnapshot): SessionEvent? {
        val connected = isConnected(snapshot.connectionStatus)
        val charging = isCharging(snapshot.chargingStatus)

        return when (state) {
            State.DISCONNECTED -> when {
                !connected -> null
                charging -> {
                    state = State.CHARGING
                    nonChargingStreak = 0
                    SessionEvent.Start(snapshot)
                }
                else -> {
                    state = State.IDLE_PLUGGED
                    null
                }
            }

            State.IDLE_PLUGGED -> when {
                !connected -> {
                    state = State.DISCONNECTED
                    null
                }
                charging -> {
                    state = State.CHARGING
                    nonChargingStreak = 0
                    SessionEvent.Start(snapshot)
                }
                else -> null
            }

            State.CHARGING -> when {
                !connected -> {
                    state = State.DISCONNECTED
                    nonChargingStreak = 0
                    SessionEvent.End(snapshot)
                }
                charging -> {
                    nonChargingStreak = 0
                    null
                }
                else -> {
                    nonChargingStreak++
                    if (nonChargingStreak >= nonChargingPollsToEnd) {
                        state = State.IDLE_PLUGGED
                        nonChargingStreak = 0
                        SessionEvent.End(snapshot)
                    } else {
                        null
                    }
                }
            }
        }
    }

    fun reset() {
        state = State.DISCONNECTED
        nonChargingStreak = 0
    }

    /**
     * Tells the detector to pick up mid-session (after a server restart that left
     * an open session row in the DB). The very next snapshot is treated as
     * continuing — no Start event will be emitted for the resumed session.
     */
    fun resumeCharging() {
        state = State.CHARGING
        nonChargingStreak = 0
    }

    private fun isConnected(status: ChargingConnectionStatus): Boolean = when (status) {
        ChargingConnectionStatus.CONNECTED_AC,
        ChargingConnectionStatus.CONNECTED_DC -> true
        else -> false
    }

    private fun isCharging(status: ChargingSystemStatus): Boolean =
        status == ChargingSystemStatus.CHARGING
}
