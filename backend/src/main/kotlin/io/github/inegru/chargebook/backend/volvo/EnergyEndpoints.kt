package io.github.inegru.chargebook.backend.volvo

/**
 * Path builders for the Volvo Energy API v2. Kept in one place so the base URL
 * stays in config and tests don't have to fake the full URL.
 */
object EnergyEndpoints {
    private const val BASE = "/energy/v2/vehicles"

    /** Composite energy state. Backed by the `energy:state:read` scope. */
    fun state(vin: String): String = "$BASE/$vin/state"

    /** Discoverability: which fields are supported on this vehicle. */
    fun capabilities(vin: String): String = "$BASE/$vin/capabilities"
}

object ConnectedVehicleEndpoints {
    private const val BASE = "/connected-vehicle/v2"

    fun vehicles(): String = "$BASE/vehicles"
}
