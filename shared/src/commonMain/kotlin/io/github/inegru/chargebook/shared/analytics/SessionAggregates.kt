package io.github.inegru.chargebook.shared.analytics

import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import kotlin.time.DurationUnit

/**
 * Computes per-session aggregates from a time-ordered list of snapshots.
 *
 * `energyKwh` is the trapezoidal integral of `powerKw × Δt`. If power readings
 * are sparse or absent, callers should fall back to `(endSoc − startSoc) ×
 * batteryCapacityKwh / 100` via [EfficiencyCalc.estimateEnergyFromSoc].
 */
object SessionAggregates {

    data class Aggregate(
        val energyKwh: Double,
        val avgPowerKw: Double,
        val peakPowerKw: Double,
        val startSocPct: Int?,
        val endSocPct: Int?,
    )

    fun compute(snapshots: List<ChargingSnapshot>): Aggregate {
        if (snapshots.isEmpty()) {
            return Aggregate(0.0, 0.0, 0.0, null, null)
        }

        val sorted = snapshots.sortedBy { it.recordedAt }
        val powered = sorted.filter { it.powerKw != null }

        var energyKwh = 0.0
        for (i in 1 until powered.size) {
            val prev = powered[i - 1]
            val cur = powered[i]
            val dtHours = (cur.recordedAt - prev.recordedAt)
                .toDouble(DurationUnit.HOURS)
            val avgPowerSegment = ((prev.powerKw ?: 0.0) + (cur.powerKw ?: 0.0)) / 2.0
            energyKwh += avgPowerSegment * dtHours
        }

        val totalHours = (sorted.last().recordedAt - sorted.first().recordedAt)
            .toDouble(DurationUnit.HOURS)
        val avgPower = if (totalHours > 0) energyKwh / totalHours else 0.0
        val peakPower = powered.mapNotNull { it.powerKw }.maxOrNull() ?: 0.0

        return Aggregate(
            energyKwh = energyKwh,
            avgPowerKw = avgPower,
            peakPowerKw = peakPower,
            startSocPct = sorted.first().socPct,
            endSocPct = sorted.last().socPct,
        )
    }
}
