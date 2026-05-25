package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.shared.api.EnergyStateDto
import io.github.inegru.chargebook.shared.api.EnergyValue
import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ChargingSystemStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * DTO → domain mappers for the Volvo Energy API v2.
 *
 * Each field is an [EnergyValue] envelope; we only read it when `status == "OK"`,
 * otherwise treat as absent. Unknown enum values fall through to `UNKNOWN` so a
 * new Volvo enum value doesn't break ingestion.
 */

fun EnergyStateDto.toSnapshot(vin: String): ChargingSnapshot {
    val recordedAt = listOfNotNull(
        batteryChargeLevel?.updatedAtInstant(),
        chargingStatus?.updatedAtInstant(),
        chargerConnectionStatus?.updatedAtInstant(),
    ).firstOrNull() ?: Clock.System.now()

    return ChargingSnapshot(
        recordedAt = recordedAt,
        vehicleVin = vin,
        sessionId = null,
        socPct = batteryChargeLevel.okValue()?.toInt(),
        powerKw = chargingPower.okValue(),
        rangeKm = electricRange.okValue()?.toInt(),
        estimatedMinutes = estimatedChargingTimeToTargetBatteryChargeLevel.okValue(),
        chargingStatus = chargingStatus.okValue().toChargingSystemStatus(),
        connectionStatus = chargerConnectionStatus.okValue().toChargingConnectionStatus(
            chargingType = chargingType.okValue(),
        ),
    )
}

private fun <T> EnergyValue<T>?.okValue(): T? = if (this?.isOk == true) value else null

private fun EnergyValue<*>?.updatedAtInstant(): Instant? =
    this?.updatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }

internal fun String?.toChargingSystemStatus(): ChargingSystemStatus = when (this) {
    "CHARGING" -> ChargingSystemStatus.CHARGING
    "IDLE" -> ChargingSystemStatus.IDLE
    "DONE" -> ChargingSystemStatus.DONE
    "SCHEDULED" -> ChargingSystemStatus.SCHEDULED
    "FAULT" -> ChargingSystemStatus.FAULT
    "UNSPECIFIED" -> ChargingSystemStatus.UNSPECIFIED
    null -> ChargingSystemStatus.UNKNOWN
    else -> ChargingSystemStatus.UNKNOWN
}

internal fun String?.toChargingConnectionStatus(
    chargingType: String?,
): ChargingConnectionStatus = when (this) {
    "DISCONNECTED" -> ChargingConnectionStatus.DISCONNECTED
    "CONNECTED" -> when (chargingType) {
        "DC" -> ChargingConnectionStatus.CONNECTED_DC
        "AC" -> ChargingConnectionStatus.CONNECTED_AC
        else -> ChargingConnectionStatus.CONNECTED_AC
    }
    "FAULT" -> ChargingConnectionStatus.FAULT
    "UNSPECIFIED" -> ChargingConnectionStatus.UNSPECIFIED
    null -> ChargingConnectionStatus.UNKNOWN
    else -> ChargingConnectionStatus.UNKNOWN
}
