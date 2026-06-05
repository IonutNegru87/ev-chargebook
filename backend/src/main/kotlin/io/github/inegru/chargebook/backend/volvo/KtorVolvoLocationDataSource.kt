package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.backend.http.safeGet
import io.github.inegru.chargebook.shared.api.LocationDto
import io.github.inegru.chargebook.shared.data.VolvoLocationDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.GeoPoint
import io.github.inegru.chargebook.shared.result.Result
import io.github.inegru.chargebook.shared.result.map
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth

class KtorVolvoLocationDataSource(
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> String,
) : VolvoLocationDataSource {

    override suspend fun currentLocation(
        vin: String,
    ): Result<GeoPoint?, DataError.Network> {
        val token = tokenProvider()
        return httpClient
            .safeGet<LocationDto>(LocationEndpoints.current(vin)) {
                bearerAuth(token)
            }
            .map { dto -> dto.toGeoPointOrNull() }
    }
}

private fun LocationDto.toGeoPointOrNull(): GeoPoint? {
    val coords = data?.geometry?.coordinates ?: return null
    if (coords.size < 2) return null
    // GeoJSON ordering: [longitude, latitude, altitude].
    return GeoPoint(lat = coords[1], lon = coords[0])
}
