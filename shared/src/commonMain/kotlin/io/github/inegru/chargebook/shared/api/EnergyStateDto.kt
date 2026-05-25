package io.github.inegru.chargebook.shared.api

import kotlinx.serialization.Serializable

/**
 * Composite response from `GET /energy/v2/vehicles/{vin}/state`.
 *
 * Every field is wrapped in [EnergyValue] which exposes a `status` ("OK" or
 * "ERROR"). When `status == "ERROR"`, `value`/`unit`/`updatedAt` are absent and
 * `code`/`message` describe why (typically `PROPERTY_NOT_SUPPORTED` for fields
 * the car never has, or `PROPERTY_NOT_FOUND` for fields temporarily without a
 * value — e.g. `chargingPower` while disconnected).
 */
@Serializable
data class EnergyStateDto(
    val batteryChargeLevel: EnergyValue<Double>? = null,
    val electricRange: EnergyValue<Double>? = null,
    val chargerConnectionStatus: EnergyValue<String>? = null,
    val chargingStatus: EnergyValue<String>? = null,
    val chargingType: EnergyValue<String>? = null,
    val chargerPowerStatus: EnergyValue<String>? = null,
    val estimatedChargingTimeToTargetBatteryChargeLevel: EnergyValue<Int>? = null,
    val chargingCurrentLimit: EnergyValue<Double>? = null,
    val targetBatteryChargeLevel: EnergyValue<Int>? = null,
    val chargingPower: EnergyValue<Double>? = null,
)

@Serializable
data class EnergyValue<T>(
    val status: String,
    val value: T? = null,
    val unit: String? = null,
    val updatedAt: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    val isOk: Boolean get() = status == "OK"
}
