package io.github.inegru.chargebook.backend.persistence

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.ConnectionType
import io.github.inegru.chargebook.shared.model.GeoPoint
import io.github.inegru.chargebook.shared.result.EmptyResult
import io.github.inegru.chargebook.shared.result.Result
import java.util.UUID
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

interface SessionLocalDataSource {
    suspend fun insert(session: ChargingSession): EmptyResult<DataError.Local>
    suspend fun update(session: ChargingSession): EmptyResult<DataError.Local>
    suspend fun get(id: String): Result<ChargingSession?, DataError.Local>
    suspend fun list(
        vehicleVin: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): Result<List<ChargingSession>, DataError.Local>
    suspend fun openSessionFor(vehicleVin: String): Result<ChargingSession?, DataError.Local>
}

class ExposedSessionDataSource : SessionLocalDataSource {

    private val log = LoggerFactory.getLogger("ExposedSessionDataSource")

    override suspend fun insert(session: ChargingSession): EmptyResult<DataError.Local> = try {
        newSuspendedTransaction {
            ChargingSessionTable.insert { it.fromSession(session) }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to insert session ${session.id}", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun update(session: ChargingSession): EmptyResult<DataError.Local> = try {
        val rows = newSuspendedTransaction {
            ChargingSessionTable.update({ ChargingSessionTable.id eq UUID.fromString(session.id) }) {
                it[endedAt] = session.endedAt
                it[startSocPct] = session.startSocPct
                it[endSocPct] = session.endSocPct
                it[energyKwh] = session.energyKwh?.toBigDecimal()
                it[avgPowerKw] = session.avgPowerKw?.toBigDecimal()
                it[peakPowerKw] = session.peakPowerKw?.toBigDecimal()
                it[connectionType] = session.connectionType.name
                it[locationLat] = session.location?.lat
                it[locationLon] = session.location?.lon
                it[locationLabel] = session.locationLabel
                it[tariffEurPerKwh] = session.tariffEurPerKwh?.toBigDecimal()
                it[costEur] = session.costEur?.toBigDecimal()
                it[solarKwh] = session.solarKwh?.toBigDecimal()
            }
        }
        if (rows == 0) Result.Error(DataError.Local.NOT_FOUND) else Result.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to update session ${session.id}", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun get(id: String): Result<ChargingSession?, DataError.Local> = try {
        val row = newSuspendedTransaction {
            ChargingSessionTable
                .selectAll()
                .where { ChargingSessionTable.id eq UUID.fromString(id) }
                .firstOrNull()
                ?.toSession()
        }
        Result.Success(row)
    } catch (e: Exception) {
        log.error("Failed to read session $id", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun list(
        vehicleVin: String?,
        since: Instant?,
        limit: Int,
    ): Result<List<ChargingSession>, DataError.Local> = try {
        val rows = newSuspendedTransaction {
            ChargingSessionTable
                .selectAll()
                .apply {
                    val v = vehicleVin
                    val s = since
                    when {
                        v != null && s != null -> where {
                            (ChargingSessionTable.vehicleVin eq v) and
                                (ChargingSessionTable.startedAt greaterEq s)
                        }
                        v != null -> where { ChargingSessionTable.vehicleVin eq v }
                        s != null -> where { ChargingSessionTable.startedAt greaterEq s }
                    }
                }
                .orderBy(ChargingSessionTable.startedAt to SortOrder.DESC)
                .limit(limit)
                .map { it.toSession() }
        }
        Result.Success(rows)
    } catch (e: Exception) {
        log.error("Failed to list sessions", e)
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun openSessionFor(
        vehicleVin: String,
    ): Result<ChargingSession?, DataError.Local> = try {
        val row = newSuspendedTransaction {
            ChargingSessionTable
                .selectAll()
                .where {
                    (ChargingSessionTable.vehicleVin eq vehicleVin) and
                        ChargingSessionTable.endedAt.isNull()
                }
                .orderBy(ChargingSessionTable.startedAt to SortOrder.DESC)
                .firstOrNull()
                ?.toSession()
        }
        Result.Success(row)
    } catch (e: Exception) {
        log.error("Failed to find open session for $vehicleVin", e)
        Result.Error(DataError.Local.UNKNOWN)
    }
}

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromSession(
    session: ChargingSession,
) {
    this[ChargingSessionTable.id] = UUID.fromString(session.id)
    this[ChargingSessionTable.vehicleVin] = session.vehicleVin
    this[ChargingSessionTable.startedAt] = session.startedAt
    this[ChargingSessionTable.endedAt] = session.endedAt
    this[ChargingSessionTable.startSocPct] = session.startSocPct
    this[ChargingSessionTable.endSocPct] = session.endSocPct
    this[ChargingSessionTable.energyKwh] = session.energyKwh?.toBigDecimal()
    this[ChargingSessionTable.avgPowerKw] = session.avgPowerKw?.toBigDecimal()
    this[ChargingSessionTable.peakPowerKw] = session.peakPowerKw?.toBigDecimal()
    this[ChargingSessionTable.connectionType] = session.connectionType.name
    this[ChargingSessionTable.locationLat] = session.location?.lat
    this[ChargingSessionTable.locationLon] = session.location?.lon
    this[ChargingSessionTable.locationLabel] = session.locationLabel
    this[ChargingSessionTable.tariffEurPerKwh] = session.tariffEurPerKwh?.toBigDecimal()
    this[ChargingSessionTable.costEur] = session.costEur?.toBigDecimal()
    this[ChargingSessionTable.solarKwh] = session.solarKwh?.toBigDecimal()
}

private fun ResultRow.toSession(): ChargingSession = ChargingSession(
    id = this[ChargingSessionTable.id].toString(),
    vehicleVin = this[ChargingSessionTable.vehicleVin],
    startedAt = this[ChargingSessionTable.startedAt],
    endedAt = this[ChargingSessionTable.endedAt],
    startSocPct = this[ChargingSessionTable.startSocPct],
    endSocPct = this[ChargingSessionTable.endSocPct],
    energyKwh = this[ChargingSessionTable.energyKwh]?.toDouble(),
    avgPowerKw = this[ChargingSessionTable.avgPowerKw]?.toDouble(),
    peakPowerKw = this[ChargingSessionTable.peakPowerKw]?.toDouble(),
    connectionType = this[ChargingSessionTable.connectionType]
        ?.let { runCatching { ConnectionType.valueOf(it) }.getOrDefault(ConnectionType.UNKNOWN) }
        ?: ConnectionType.UNKNOWN,
    location = this[ChargingSessionTable.locationLat]?.let { lat ->
        this[ChargingSessionTable.locationLon]?.let { lon -> GeoPoint(lat, lon) }
    },
    locationLabel = this[ChargingSessionTable.locationLabel],
    tariffEurPerKwh = this[ChargingSessionTable.tariffEurPerKwh]?.toDouble(),
    costEur = this[ChargingSessionTable.costEur]?.toDouble(),
    solarKwh = this[ChargingSessionTable.solarKwh]?.toDouble(),
)
