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
}
