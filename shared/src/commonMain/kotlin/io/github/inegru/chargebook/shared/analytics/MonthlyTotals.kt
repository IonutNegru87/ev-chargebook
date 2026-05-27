package io.github.inegru.chargebook.shared.analytics

import kotlinx.serialization.Serializable

/**
 * Per-month charging aggregate. Returned by `GET /api/analytics/monthly` and
 * consumed by the web analytics screen, so it lives in `:shared`.
 *
 * `billableKwh = energyKwh − solarKwh`. `costEur` is the sum of each session's
 * stored cost.
 */
@Serializable
data class MonthlyTotals(
    val year: Int,
    val month: Int,
    val sessions: Int,
    val energyKwh: Double,
    val solarKwh: Double,
    val billableKwh: Double,
    val costEur: Double,
)
