package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the Volvo Energy API `charging-system-status` enum.
 *
 * Volvo returns a string; we keep `UNKNOWN` as a forward-compatible sink for values
 * the EX30 firmware adds later.
 */
@Serializable
enum class ChargingSystemStatus {
    @SerialName("CHARGING_SYSTEM_CHARGING")
    CHARGING,

    @SerialName("CHARGING_SYSTEM_IDLE")
    IDLE,

    @SerialName("CHARGING_SYSTEM_DONE")
    DONE,

    @SerialName("CHARGING_SYSTEM_SCHEDULED")
    SCHEDULED,

    @SerialName("CHARGING_SYSTEM_FAULT")
    FAULT,

    @SerialName("CHARGING_SYSTEM_UNSPECIFIED")
    UNSPECIFIED,

    UNKNOWN,
}
