package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val vin: String,
    val modelName: String,
    val modelYear: Int? = null,
    val batteryCapacityKwh: Double? = null,
)
