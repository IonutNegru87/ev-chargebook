package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChargingConnectionStatus {
    @SerialName("CONNECTION_STATUS_CONNECTED_AC")
    CONNECTED_AC,

    @SerialName("CONNECTION_STATUS_CONNECTED_DC")
    CONNECTED_DC,

    @SerialName("CONNECTION_STATUS_DISCONNECTED")
    DISCONNECTED,

    @SerialName("CONNECTION_STATUS_FAULT")
    FAULT,

    @SerialName("CONNECTION_STATUS_UNSPECIFIED")
    UNSPECIFIED,

    UNKNOWN,
}
