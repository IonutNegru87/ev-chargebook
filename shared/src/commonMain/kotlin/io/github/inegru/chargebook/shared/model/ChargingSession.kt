package io.github.inegru.chargebook.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

enum class ConnectionType { AC, DC, UNKNOWN }

@Serializable
data class GeoPoint(
    val lat: Double,
    val lon: Double,
)

/**
 * A detected charging session — one row per plug-in-to-plug-out cycle with charging
 * actually observed. See [io.github.inegru.chargebook.shared.analytics.SessionAggregates]
 * for how energy/cost are derived from snapshots.
 */
@Serializable
data class ChargingSession(
    val id: String,
    val vehicleVin: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val startSocPct: Int? = null,
    val endSocPct: Int? = null,
    val energyKwh: Double? = null,
    val avgPowerKw: Double? = null,
    val peakPowerKw: Double? = null,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val location: GeoPoint? = null,
    val locationLabel: String? = null,
    val tariffEurPerKwh: Double? = null,
    val costEur: Double? = null,
    /**
     * Portion of [energyKwh] supplied by a free source (e.g. home solar). Does
     * not contribute to [costEur]. If the user enters a percentage at the API,
     * it's converted to kWh against [energyKwh] before being stored.
     */
    val solarKwh: Double? = null,
)
