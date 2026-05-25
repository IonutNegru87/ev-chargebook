package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the Volvo Energy API v2 `chargingStatus.value` enum.
 *
 * `UNKNOWN` is a forward-compatible sink for values the EX30 firmware adds later.
 */
@Serializable
enum class ChargingSystemStatus {
    @SerialName("CHARGING")
    CHARGING,

    @SerialName("IDLE")
    IDLE,

    @SerialName("DONE")
    DONE,

    @SerialName("SCHEDULED")
    SCHEDULED,

    @SerialName("FAULT")
    FAULT,

    @SerialName("UNSPECIFIED")
    UNSPECIFIED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
