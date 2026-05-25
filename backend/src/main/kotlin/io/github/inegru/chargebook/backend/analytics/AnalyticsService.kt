package io.github.inegru.chargebook.backend.analytics

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyTotals(
    val year: Int,
    val month: Int,
    val sessions: Int,
    val energyKwh: Double,
    val costEur: Double,
)

class AnalyticsService {
    suspend fun monthly(vehicleVin: String, from: LocalDate, to: LocalDate): List<MonthlyTotals> = TODO()
}
