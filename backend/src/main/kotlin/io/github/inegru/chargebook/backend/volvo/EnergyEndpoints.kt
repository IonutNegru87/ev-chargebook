package io.github.inegru.chargebook.backend.volvo

/**
 * Path builders for the Volvo Energy API v2. Kept in one place so the base URL
 * stays in config and tests don't have to fake the full URL.
 */
object EnergyEndpoints {
    private const val BASE = "/energy/v2/vehicles"

    fun rechargeStatus(vin: String): String = "$BASE/$vin/recharge-status"
    fun batteryChargeLevel(vin: String): String = "$BASE/$vin/battery-charge-level"
    fun electricRange(vin: String): String = "$BASE/$vin/electric-range"
    fun estimatedChargingTime(vin: String): String = "$BASE/$vin/estimated-charging-time"
    fun chargingConnectionStatus(vin: String): String = "$BASE/$vin/charging-connection-status"
    fun chargingSystemStatus(vin: String): String = "$BASE/$vin/charging-system-status"
    fun chargingPower(vin: String): String = "$BASE/$vin/charging-power"
}

object ConnectedVehicleEndpoints {
    private const val BASE = "/connected-vehicle/v2"

    fun vehicles(): String = "$BASE/vehicles"
}
