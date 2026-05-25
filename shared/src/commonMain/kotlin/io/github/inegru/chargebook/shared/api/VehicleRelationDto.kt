package io.github.inegru.chargebook.shared.api

import kotlinx.serialization.Serializable

/**
 * Response shape for `GET /connected-vehicle/v2/vehicles`. Volvo wraps lists in a
 * `data` envelope across most of their newer APIs.
 */
@Serializable
data class VehicleRelationListDto(
    val data: List<VehicleRelationItemDto> = emptyList(),
)

@Serializable
data class VehicleRelationItemDto(
    val vin: String,
)
