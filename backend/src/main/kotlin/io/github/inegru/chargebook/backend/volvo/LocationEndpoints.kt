package io.github.inegru.chargebook.backend.volvo

object LocationEndpoints {
    private const val BASE = "/location/v1/vehicles"

    /** Returns a GeoJSON `Feature` wrapped in `{ status, operationId, data }`. */
    fun current(vin: String): String = "$BASE/$vin/location"
}
