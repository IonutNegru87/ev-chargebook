package io.github.inegru.chargebook.backend.poller

import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSystemStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Adaptive cadence chosen to stay well inside the documented quota
 * (100 req/min, 10k/day) regardless of how long a session runs.
 *
 * See [EX30-Charging-History-Feasibility.md, §3].
 */
object PollingScheduler {

    fun intervalFor(
        connection: ChargingConnectionStatus,
        system: ChargingSystemStatus,
    ): Duration = when {
        connection == ChargingConnectionStatus.DISCONNECTED -> 30.minutes
        system == ChargingSystemStatus.CHARGING -> 60.seconds
        else -> 5.minutes
    }
}
