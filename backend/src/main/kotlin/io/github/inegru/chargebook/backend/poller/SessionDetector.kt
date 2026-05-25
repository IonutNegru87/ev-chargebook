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
 * ANY          --(DISCONNECTED)-->      DISCONNECTED   [SessionEnd if active]
 * ```
 *
 * `N` (debounce) is `nonChargingPollsToEnd` and exists because the EX30
 * intermittently reports `IDLE` or `FAULT` mid-session.
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
        TODO("Drive the state machine and emit SessionEvent.Start/End. Reset nonChargingStreak on every CHARGING tick.")
    }

    fun reset() {
        state = State.DISCONNECTED
        nonChargingStreak = 0
    }

    @Suppress("unused")
    private fun isConnected(status: ChargingConnectionStatus): Boolean = when (status) {
        ChargingConnectionStatus.CONNECTED_AC,
        ChargingConnectionStatus.CONNECTED_DC -> true
        else -> false
    }

    @Suppress("unused")
    private fun isCharging(status: ChargingSystemStatus): Boolean =
        status == ChargingSystemStatus.CHARGING
}
