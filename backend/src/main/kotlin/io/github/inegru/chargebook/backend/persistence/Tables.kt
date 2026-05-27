package io.github.inegru.chargebook.backend.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Exposed mappings for the schema created by Flyway in
 * [V1__init.sql](../../resources/db/migration/V1__init.sql).
 *
 * Exposed does not create or migrate these tables — Flyway is the single source
 * of truth for the schema. Column names and types here must stay in sync with
 * the migration.
 */

object ChargingSessionTable : Table("charging_session") {
    val id = uuid("id")
    val vehicleVin = text("vehicle_vin")
    val startedAt = timestamp("started_at")
    val endedAt = timestamp("ended_at").nullable()
    val startSocPct = integer("start_soc_pct").nullable()
    val endSocPct = integer("end_soc_pct").nullable()
    val energyKwh = decimal("energy_kwh", 6, 3).nullable()
    val avgPowerKw = decimal("avg_power_kw", 6, 3).nullable()
    val peakPowerKw = decimal("peak_power_kw", 6, 3).nullable()
    val connectionType = text("connection_type").nullable()
    val locationLat = double("location_lat").nullable()
    val locationLon = double("location_lon").nullable()
    val locationLabel = text("location_label").nullable()
    val tariffEurPerKwh = decimal("tariff_eur_kwh", 5, 4).nullable()
    val costEur = decimal("cost_eur", 8, 2).nullable()
    val solarKwh = decimal("solar_kwh", 6, 3).nullable()

    override val primaryKey = PrimaryKey(id)
}

object ChargingSnapshotTable : Table("charging_snapshot") {
    val recordedAt = timestamp("recorded_at")
    val sessionId = uuid("session_id").references(ChargingSessionTable.id).nullable()
    val vehicleVin = text("vehicle_vin")
    val socPct = integer("soc_pct").nullable()
    val targetSocPct = integer("target_soc_pct").nullable()
    val powerKw = decimal("power_kw", 6, 3).nullable()
    val rangeKm = integer("range_km").nullable()
    val estimatedMinutes = integer("estimated_minutes").nullable()
    val chargingStatus = text("charging_status").nullable()
    val connectionStatus = text("connection_status").nullable()
    val ingestedAt = timestamp("ingested_at")
}
