package io.github.inegru.chargebook.backend.persistence

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.result.EmptyResult
import io.github.inegru.chargebook.shared.result.Result
import kotlinx.datetime.Instant

interface SnapshotLocalDataSource {
    suspend fun insert(snapshot: ChargingSnapshot): EmptyResult<DataError.Local>
    suspend fun forSession(sessionId: String): Result<List<ChargingSnapshot>, DataError.Local>
    suspend fun latestFor(vehicleVin: String): Result<ChargingSnapshot?, DataError.Local>
    suspend fun between(
        vehicleVin: String,
        from: Instant,
        to: Instant,
    ): Result<List<ChargingSnapshot>, DataError.Local>
}

class ExposedSnapshotDataSource : SnapshotLocalDataSource {
    override suspend fun insert(snapshot: ChargingSnapshot): EmptyResult<DataError.Local> = TODO()
    override suspend fun forSession(sessionId: String): Result<List<ChargingSnapshot>, DataError.Local> = TODO()
    override suspend fun latestFor(vehicleVin: String): Result<ChargingSnapshot?, DataError.Local> = TODO()
    override suspend fun between(
        vehicleVin: String,
        from: Instant,
        to: Instant,
    ): Result<List<ChargingSnapshot>, DataError.Local> = TODO()
}
