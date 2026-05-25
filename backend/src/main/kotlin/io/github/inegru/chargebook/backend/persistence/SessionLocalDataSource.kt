package io.github.inegru.chargebook.backend.persistence

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.result.EmptyResult
import io.github.inegru.chargebook.shared.result.Result
import kotlinx.datetime.Instant

/**
 * Local persistence for [ChargingSession]. Single source → data source (not
 * "repository") per the android-data-layer convention.
 */
interface SessionLocalDataSource {
    suspend fun insert(session: ChargingSession): EmptyResult<DataError.Local>
    suspend fun update(session: ChargingSession): EmptyResult<DataError.Local>
    suspend fun get(id: String): Result<ChargingSession, DataError.Local>
    suspend fun list(
        vehicleVin: String,
        since: Instant? = null,
        limit: Int = 100,
    ): Result<List<ChargingSession>, DataError.Local>
    suspend fun openSessionFor(vehicleVin: String): Result<ChargingSession?, DataError.Local>
}

class ExposedSessionDataSource : SessionLocalDataSource {
    override suspend fun insert(session: ChargingSession): EmptyResult<DataError.Local> = TODO()
    override suspend fun update(session: ChargingSession): EmptyResult<DataError.Local> = TODO()
    override suspend fun get(id: String): Result<ChargingSession, DataError.Local> = TODO()
    override suspend fun list(
        vehicleVin: String,
        since: Instant?,
        limit: Int,
    ): Result<List<ChargingSession>, DataError.Local> = TODO()
    override suspend fun openSessionFor(vehicleVin: String): Result<ChargingSession?, DataError.Local> = TODO()
}
