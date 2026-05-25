package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.backend.http.safeGet
import io.github.inegru.chargebook.shared.api.VehicleRelationListDto
import io.github.inegru.chargebook.shared.data.VolvoVehiclesDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.result.Result
import io.github.inegru.chargebook.shared.result.map
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth

class KtorVolvoVehiclesDataSource(
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> String,
) : VolvoVehiclesDataSource {

    override suspend fun list(): Result<List<String>, DataError.Network> {
        val token = tokenProvider()
        return httpClient
            .safeGet<VehicleRelationListDto>(ConnectedVehicleEndpoints.vehicles()) {
                bearerAuth(token)
            }
            .map { dto -> dto.data.map { it.vin } }
    }
}
