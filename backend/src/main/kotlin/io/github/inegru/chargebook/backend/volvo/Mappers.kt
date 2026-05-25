package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.shared.api.RechargeStatusDto
import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ChargingSystemStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * DTO → domain mappers for the Volvo Energy API.
 *
 * Extension functions co-located with the DTOs they map from, per the
 * android-data-layer convention. Unknown enum values fall through to `UNKNOWN`
 * rather than throwing so a single new Volvo enum value doesn't break ingestion.
 */

fun RechargeStatusDto.toSnapshot(vin: String): ChargingSnapshot {
    val recordedAt = listOfNotNull(
        batteryChargeLevel?.timestamp,
        chargingPower?.timestamp,
        chargingSystemStatus?.timestamp,
    ).firstNotNullOfOrNull { runCatching { Instant.parse(it) }.getOrNull() }
        ?: Clock.System.now()

    return ChargingSnapshot(
        recordedAt = recordedAt,
        vehicleVin = vin,
        sessionId = null,
        socPct = batteryChargeLevel?.value?.toInt(),
        powerKw = chargingPower?.value,
        rangeKm = electricRange?.value,
        estimatedMinutes = estimatedChargingTime?.value,
        chargingStatus = chargingSystemStatus?.value.toChargingSystemStatus(),
        connectionStatus = chargingConnectionStatus?.value.toChargingConnectionStatus(),
    )
}

internal fun String?.toChargingSystemStatus(): ChargingSystemStatus = when (this) {
    "CHARGING_SYSTEM_CHARGING" -> ChargingSystemStatus.CHARGING
    "CHARGING_SYSTEM_IDLE" -> ChargingSystemStatus.IDLE
    "CHARGING_SYSTEM_DONE" -> ChargingSystemStatus.DONE
    "CHARGING_SYSTEM_SCHEDULED" -> ChargingSystemStatus.SCHEDULED
    "CHARGING_SYSTEM_FAULT" -> ChargingSystemStatus.FAULT
    "CHARGING_SYSTEM_UNSPECIFIED" -> ChargingSystemStatus.UNSPECIFIED
    else -> ChargingSystemStatus.UNKNOWN
}

internal fun String?.toChargingConnectionStatus(): ChargingConnectionStatus = when (this) {
    "CONNECTION_STATUS_CONNECTED_AC" -> ChargingConnectionStatus.CONNECTED_AC
    "CONNECTION_STATUS_CONNECTED_DC" -> ChargingConnectionStatus.CONNECTED_DC
    "CONNECTION_STATUS_DISCONNECTED" -> ChargingConnectionStatus.DISCONNECTED
    "CONNECTION_STATUS_FAULT" -> ChargingConnectionStatus.FAULT
    "CONNECTION_STATUS_UNSPECIFIED" -> ChargingConnectionStatus.UNSPECIFIED
    else -> ChargingConnectionStatus.UNKNOWN
}
