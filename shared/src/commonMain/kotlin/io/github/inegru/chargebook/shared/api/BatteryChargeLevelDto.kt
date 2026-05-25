package io.github.inegru.chargebook.shared.api

import kotlinx.serialization.Serializable

/** Response from `GET /energy/v2/vehicles/{vin}/battery-charge-level`. */
@Serializable
data class BatteryChargeLevelDto(
    val value: Double,
    val unit: String,
    val timestamp: String,
)
