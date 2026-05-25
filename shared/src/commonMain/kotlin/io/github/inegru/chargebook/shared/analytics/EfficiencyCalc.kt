package io.github.inegru.chargebook.shared.analytics

object EfficiencyCalc {

    /** EX30 Twin Motor Performance — ~64 kWh usable. */
    const val EX30_TWIN_USABLE_KWH: Double = 64.0

    /** EX30 Single Motor RWD — ~49 kWh usable (LFP pack). */
    const val EX30_SINGLE_RWD_USABLE_KWH: Double = 49.0

    /**
     * Fallback energy estimate when power-over-time integration is unreliable
     * (sparse or missing `chargingPower` samples). Returns `null` if either SoC
     * is missing.
     */
    fun estimateEnergyFromSoc(
        startSocPct: Int?,
        endSocPct: Int?,
        usableCapacityKwh: Double,
    ): Double? {
        if (startSocPct == null || endSocPct == null) return null
        val delta = (endSocPct - startSocPct).coerceAtLeast(0)
        return delta / 100.0 * usableCapacityKwh
    }

    fun costEur(energyKwh: Double, tariffEurPerKwh: Double): Double =
        energyKwh * tariffEurPerKwh

    /**
     * Cost net of solar: the kWh paid for is `energyKwh − solarKwh` (clamped to
     * ≥ 0 to guard against user input that overshoots). Returns `null` when no
     * tariff is set — we don't pretend a missing tariff means free.
     */
    fun costEurNetOfSolar(
        energyKwh: Double,
        solarKwh: Double?,
        tariffEurPerKwh: Double?,
    ): Double? {
        if (tariffEurPerKwh == null) return null
        val billable = (energyKwh - (solarKwh ?: 0.0)).coerceAtLeast(0.0)
        return billable * tariffEurPerKwh
    }

    /** Converts a percentage (0..100) into kWh of [energyKwh]. */
    fun solarKwhFromPct(energyKwh: Double, pct: Double): Double =
        energyKwh * pct.coerceIn(0.0, 100.0) / 100.0
}
