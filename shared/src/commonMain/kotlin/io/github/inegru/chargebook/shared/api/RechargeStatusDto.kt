package io.github.inegru.chargebook.shared.api

import kotlinx.serialization.Serializable

/**
 * Composite response from `GET /energy/v2/vehicles/{vin}/recharge-status`.
 *
 * Fields are nullable because the EX30 does not return every value (e.g.
 * `targetBatteryChargeLevel` and `chargingCurrentLimit` are documented as
 * unsupported). Volvo also returns each field wrapped in a `{ value, timestamp }`
 * envelope — see [ValueWithTimestamp].
 */
@Serializable
data class RechargeStatusDto(
    val batteryChargeLevel: ValueWithTimestamp<Double>? = null,
    val electricRange: ValueWithTimestamp<Int>? = null,
    val estimatedChargingTime: ValueWithTimestamp<Int>? = null,
    val chargingConnectionStatus: ValueWithTimestamp<String>? = null,
    val chargingSystemStatus: ValueWithTimestamp<String>? = null,
    val chargingPower: ValueWithTimestamp<Double>? = null,
)

@Serializable
data class ValueWithTimestamp<T>(
    val value: T,
    val timestamp: String,
    val unit: String? = null,
)
