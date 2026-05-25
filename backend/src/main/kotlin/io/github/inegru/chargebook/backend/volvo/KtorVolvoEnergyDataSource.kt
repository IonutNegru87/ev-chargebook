package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.backend.http.safeGet
import io.github.inegru.chargebook.shared.api.RechargeStatusDto
import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.result.Result
import io.github.inegru.chargebook.shared.result.map
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth

/**
 * Ktor-backed implementation of [VolvoEnergyDataSource].
 *
 * The bearer token is fetched per call via [tokenProvider] so refresh logic lives
 * in one place (the auth layer) rather than being hard-wired into the client.
 */
class KtorVolvoEnergyDataSource(
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> String,
) : VolvoEnergyDataSource {

    override suspend fun rechargeStatus(vin: String): Result<ChargingSnapshot, DataError.Network> {
        val token = tokenProvider()
        return httpClient
            .safeGet<RechargeStatusDto>(EnergyEndpoints.rechargeStatus(vin)) {
                bearerAuth(token)
            }
            .map { dto -> dto.toSnapshot(vin) }
    }
}
