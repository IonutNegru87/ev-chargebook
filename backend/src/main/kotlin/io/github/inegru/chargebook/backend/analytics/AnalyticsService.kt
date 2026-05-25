package io.github.inegru.chargebook.backend.analytics

import io.github.inegru.chargebook.backend.persistence.SessionLocalDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.result.Result
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

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

/**
 * Monthly aggregation over [ChargingSession] rows. Bucketed in the system
 * timezone — the user thinks in calendar months, not UTC days.
 *
 * `billableKwh = energyKwh − solarKwh`. `costEur` is the value already stored
 * on each session (computed at session-close from tariff × billable), summed.
 */
class AnalyticsService(private val sessions: SessionLocalDataSource) {

    suspend fun monthly(
        vehicleVin: String?,
        from: LocalDate?,
        to: LocalDate?,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<List<MonthlyTotals>, DataError.Local> {
        val sinceInstant: Instant? = from?.atStartOfDayIn(timeZone)
        val toInstant: Instant? = to?.let {
            // End-of-day in the requested TZ.
            it.atTime(LocalTime(23, 59, 59)).toInstant(timeZone)
        }

        return when (val r = sessions.list(vehicleVin = vehicleVin, since = sinceInstant, limit = 10_000)) {
            is Result.Error -> Result.Error(r.error)
            is Result.Success -> Result.Success(aggregate(r.data, toInstant, timeZone))
        }
    }

    private fun aggregate(
        sessions: List<ChargingSession>,
        upTo: Instant?,
        timeZone: TimeZone,
    ): List<MonthlyTotals> {
        val filtered = if (upTo == null) sessions else sessions.filter { it.startedAt <= upTo }
        return filtered
            .groupBy {
                val ld = it.startedAt.toLocalDateTime(timeZone).date
                ld.year to ld.monthNumber
            }
            .map { (yearMonth, rows) ->
                val (year, month) = yearMonth
                val energy = rows.sumOf { it.energyKwh ?: 0.0 }
                val solar = rows.sumOf { it.solarKwh ?: 0.0 }
                MonthlyTotals(
                    year = year,
                    month = month,
                    sessions = rows.size,
                    energyKwh = energy,
                    solarKwh = solar,
                    billableKwh = (energy - solar).coerceAtLeast(0.0),
                    costEur = rows.sumOf { it.costEur ?: 0.0 },
                )
            }
            .sortedWith(compareByDescending<MonthlyTotals> { it.year }.thenByDescending { it.month })
    }

    private fun LocalDate.atTime(time: LocalTime): kotlinx.datetime.LocalDateTime =
        kotlinx.datetime.LocalDateTime(this, time)
}
