package io.github.inegru.chargebook.shared.data

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.GeoPoint
import io.github.inegru.chargebook.shared.result.Result

/**
 * Current GPS position for a vehicle. Success-with-null means the API
 * responded but reported no coordinates (e.g. vehicle offline); a `Result.Error`
 * is reserved for transport / auth failures.
 */
interface VolvoLocationDataSource {
    suspend fun currentLocation(vin: String): Result<GeoPoint?, DataError.Network>
}
