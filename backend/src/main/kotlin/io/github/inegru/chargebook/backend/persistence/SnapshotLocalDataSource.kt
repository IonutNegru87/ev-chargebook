package io.github.inegru.chargebook.backend.persistence

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingConnectionStatus
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.model.ChargingSystemStatus
import io.github.inegru.chargebook.shared.model.GeoPoint
import io.github.inegru.chargebook.shared.result.EmptyResult
import io.github.inegru.chargebook.shared.result.Result
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

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

    private val log = LoggerFactory.getLogger("ExposedSnapshotDataSource")

    override suspend fun insert(snapshot: ChargingSnapshot): EmptyResult<DataError.Local> = try {
        newSuspendedTransaction {
            ChargingSnapshotTable.insert {
                it[recordedAt] = snapshot.recordedAt
                it[sessionId] = snapshot.sessionId?.let(UUID::fromString)
                it[vehicleVin] = snapshot.vehicleVin
                it[socPct] = snapshot.socPct
                it[targetSocPct] = snapshot.targetSocPct
                it[powerKw] = snapshot.powerKw?.toBigDecimal()
                it[rangeKm] = snapshot.rangeKm
                it[estimatedMinutes] = snapshot.estimatedMinutes
                it[chargingStatus] = snapshot.chargingStatus.name
                it[connectionStatus] = snapshot.connectionStatus.name
                it[ingestedAt] = Clock.System.now()
                it[locationLat] = snapshot.location?.lat
                it[locationLon] = snapshot.location?.lon
                it[locationLabel] = snapshot.locationLabel
            }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to insert snapshot for ${snapshot.vehicleVin}", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun forSession(sessionId: String): Result<List<ChargingSnapshot>, DataError.Local> = try {
        val rows = newSuspendedTransaction {
            ChargingSnapshotTable
                .selectAll()
                .where { ChargingSnapshotTable.sessionId eq UUID.fromString(sessionId) }
                .orderBy(ChargingSnapshotTable.recordedAt to SortOrder.ASC)
                .map { it.toSnapshot() }
        }
        Result.Success(rows)
    } catch (e: Exception) {
        log.error("Failed to read snapshots for session $sessionId", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun latestFor(vehicleVin: String): Result<ChargingSnapshot?, DataError.Local> = try {
        val row = newSuspendedTransaction {
            ChargingSnapshotTable
                .selectAll()
                .where { ChargingSnapshotTable.vehicleVin eq vehicleVin }
                .orderBy(ChargingSnapshotTable.ingestedAt to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.toSnapshot()
        }
        Result.Success(row)
    } catch (e: Exception) {
        log.error("Failed to read latest snapshot for $vehicleVin", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun between(
        vehicleVin: String,
        from: Instant,
        to: Instant,
    ): Result<List<ChargingSnapshot>, DataError.Local> = try {
        val rows = newSuspendedTransaction {
            ChargingSnapshotTable
                .selectAll()
                .where {
                    (ChargingSnapshotTable.vehicleVin eq vehicleVin) and
                        (ChargingSnapshotTable.recordedAt greaterEq from) and
                        (ChargingSnapshotTable.recordedAt lessEq to)
                }
                .orderBy(ChargingSnapshotTable.recordedAt to SortOrder.ASC)
                .map { it.toSnapshot() }
        }
        Result.Success(rows)
    } catch (e: Exception) {
        log.error("Failed to read snapshots for $vehicleVin between $from and $to", e)
        Result.Error(DataError.Local.UNKNOWN)
    }
}

private fun ResultRow.toSnapshot(): ChargingSnapshot = ChargingSnapshot(
    recordedAt = this[ChargingSnapshotTable.recordedAt],
    vehicleVin = this[ChargingSnapshotTable.vehicleVin],
    sessionId = this[ChargingSnapshotTable.sessionId]?.toString(),
    socPct = this[ChargingSnapshotTable.socPct],
    targetSocPct = this[ChargingSnapshotTable.targetSocPct],
    powerKw = this[ChargingSnapshotTable.powerKw]?.toDouble(),
    rangeKm = this[ChargingSnapshotTable.rangeKm],
    estimatedMinutes = this[ChargingSnapshotTable.estimatedMinutes],
    chargingStatus = this[ChargingSnapshotTable.chargingStatus]
        ?.let { runCatching { ChargingSystemStatus.valueOf(it) }.getOrDefault(ChargingSystemStatus.UNKNOWN) }
        ?: ChargingSystemStatus.UNKNOWN,
    connectionStatus = this[ChargingSnapshotTable.connectionStatus]
        ?.let { runCatching { ChargingConnectionStatus.valueOf(it) }.getOrDefault(ChargingConnectionStatus.UNKNOWN) }
        ?: ChargingConnectionStatus.UNKNOWN,
    location = this[ChargingSnapshotTable.locationLat]?.let { lat ->
        this[ChargingSnapshotTable.locationLon]?.let { lon -> GeoPoint(lat = lat, lon = lon) }
    },
    locationLabel = this[ChargingSnapshotTable.locationLabel],
)
