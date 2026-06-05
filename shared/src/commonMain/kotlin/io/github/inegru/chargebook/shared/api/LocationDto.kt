package io.github.inegru.chargebook.shared.api

import kotlinx.serialization.Serializable

/**
 * Response from `GET /location/v1/vehicles/{vin}/location` on the Volvo
 * Location API. Wraps a GeoJSON `Feature` in `{ status, operationId, data }`.
 *
 * Coordinates follow the GeoJSON convention: **[longitude, latitude, altitude]**
 * — that ordering is mandated by the spec, not the obvious lat-first one.
 */
@Serializable
data class LocationDto(
    val status: Int? = null,
    val operationId: String? = null,
    val data: LocationFeatureDto? = null,
)

@Serializable
data class LocationFeatureDto(
    val type: String? = null,
    val properties: LocationPropertiesDto? = null,
    val geometry: LocationGeometryDto? = null,
)

@Serializable
data class LocationPropertiesDto(
    val heading: String? = null,
    val timestamp: String? = null,
)

@Serializable
data class LocationGeometryDto(
    val type: String? = null,
    val coordinates: List<Double>? = null,
)
