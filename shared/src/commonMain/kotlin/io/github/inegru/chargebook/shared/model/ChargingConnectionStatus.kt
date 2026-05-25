package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Composite of the Volvo Energy API v2 `chargerConnectionStatus` and
 * `chargingType` fields. The wire format splits "is the cable plugged in" from
 * "what kind of charging", but downstream code (session detection, history)
 * cares about both together, so we collapse them into one enum here.
 */
@Serializable
enum class ChargingConnectionStatus {
    @SerialName("CONNECTED_AC")
    CONNECTED_AC,

    @SerialName("CONNECTED_DC")
    CONNECTED_DC,

    @SerialName("DISCONNECTED")
    DISCONNECTED,

    @SerialName("FAULT")
    FAULT,

    @SerialName("UNSPECIFIED")
    UNSPECIFIED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
