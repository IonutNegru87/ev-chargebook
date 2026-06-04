package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.Serializable

/**
 * Pricing override for a single session. Sent by the web app to
 * `PATCH /api/sessions/{id}` and consumed by the backend.
 *
 * `solarKwh` wins if both are present; `solarPct` is converted against the
 * session's `energyKwh` and stored as kWh. Sending an explicit `null` clears a
 * previously stored field.
 */
@Serializable
data class SessionPricingPatch(
    val tariffEurPerKwh: Double? = null,
    val solarKwh: Double? = null,
    val solarPct: Double? = null,
)
