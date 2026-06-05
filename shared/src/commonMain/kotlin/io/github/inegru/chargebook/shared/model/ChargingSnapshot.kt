package io.github.inegru.chargebook.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * One point sampled from the Volvo Energy API. Stored verbatim so we can re-derive
 * session aggregates if our logic changes later.
 */
@Serializable
data class ChargingSnapshot(
    val recordedAt: Instant,
    val vehicleVin: String,
    val sessionId: String? = null,
    val socPct: Int?,
    val targetSocPct: Int? = null,
    val powerKw: Double?,
    val rangeKm: Int?,
    val estimatedMinutes: Int?,
    val chargingStatus: ChargingSystemStatus,
    val connectionStatus: ChargingConnectionStatus,
    val location: GeoPoint? = null,
    val locationLabel: String? = null,
)
