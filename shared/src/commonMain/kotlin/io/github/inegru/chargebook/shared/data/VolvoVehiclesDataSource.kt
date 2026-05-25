package io.github.inegru.chargebook.shared.data

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.result.Result

/**
 * Lists the VINs the authenticated user has access to. Backed by the
 * `conve:vehicle_relation` scope and the Connected Vehicle API's
 * `/connected-vehicle/v2/vehicles` endpoint.
 */
interface VolvoVehiclesDataSource {
    suspend fun list(): Result<List<String>, DataError.Network>
}
