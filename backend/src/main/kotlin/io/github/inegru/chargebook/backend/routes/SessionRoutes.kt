package io.github.inegru.chargebook.backend.routes

import io.github.inegru.chargebook.backend.persistence.SessionLocalDataSource
import io.github.inegru.chargebook.backend.persistence.SnapshotLocalDataSource
import io.github.inegru.chargebook.shared.analytics.EfficiencyCalc
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.SessionPricingPatch
import io.github.inegru.chargebook.shared.model.SessionWithSnapshots
import io.github.inegru.chargebook.shared.result.Result
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import kotlinx.datetime.Instant

fun Route.sessionRoutes(
    sessions: SessionLocalDataSource,
    snapshots: SnapshotLocalDataSource,
) {
    route("/api/sessions") {

        get {
            val vin = call.request.queryParameters["vin"]
            val since = call.request.queryParameters["since"]
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            when (val r = sessions.list(vehicleVin = vin, since = since, limit = limit)) {
                is Result.Error -> call.respondText(
                    "Local store error: ${r.error}",
                    status = HttpStatusCode.InternalServerError,
                )
                is Result.Success -> call.respond(r.data)
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respondText("missing id", status = HttpStatusCode.BadRequest)
            when (val sessionR = sessions.get(id)) {
                is Result.Error -> call.respondText(
                    "Local store error: ${sessionR.error}",
                    status = HttpStatusCode.InternalServerError,
                )
                is Result.Success -> {
                    val session = sessionR.data
                        ?: return@get call.respondText(
                            "Session not found",
                            status = HttpStatusCode.NotFound,
                        )
                    when (val snapsR = snapshots.forSession(id)) {
                        is Result.Error -> call.respondText(
                            "Local store error: ${snapsR.error}",
                            status = HttpStatusCode.InternalServerError,
                        )
                        is Result.Success -> call.respond(
                            SessionWithSnapshots(session = session, snapshots = snapsR.data),
                        )
                    }
                }
            }
        }

        patch("/{id}") {
            val id = call.parameters["id"]
                ?: return@patch call.respondText("missing id", status = HttpStatusCode.BadRequest)
            val body = runCatching { call.receive<SessionPricingPatch>() }.getOrNull()
                ?: return@patch call.respondText(
                    "invalid request body",
                    status = HttpStatusCode.BadRequest,
                )
            val sessionR = sessions.get(id)
            if (sessionR is Result.Error) {
                return@patch call.respondText(
                    "Local store error: ${sessionR.error}",
                    status = HttpStatusCode.InternalServerError,
                )
            }
            val current = (sessionR as Result.Success).data
                ?: return@patch call.respondText("Session not found", status = HttpStatusCode.NotFound)

            val updated = current.applyPricing(body)
            when (val r = sessions.update(updated)) {
                is Result.Error -> call.respondText(
                    "Local store error: ${r.error}",
                    status = HttpStatusCode.InternalServerError,
                )
                is Result.Success -> call.respond(updated)
            }
        }
    }

    get("/api/sessions.csv") {
        val vin = call.request.queryParameters["vin"]
        when (val r = sessions.list(vehicleVin = vin, since = null, limit = 10_000)) {
            is Result.Error -> call.respondText(
                "Local store error: ${r.error}",
                status = HttpStatusCode.InternalServerError,
            )
            is Result.Success -> call.respondText(
                contentType = ContentType.Text.CSV,
                text = r.data.toCsv(),
            )
        }
    }
}

private fun ChargingSession.applyPricing(patch: SessionPricingPatch): ChargingSession {
    val patchKwh = patch.solarKwh
    val patchPct = patch.solarPct
    val resolvedSolarKwh: Double? = when {
        patchKwh != null -> patchKwh
        patchPct != null -> energyKwh?.let { EfficiencyCalc.solarKwhFromPct(it, patchPct) }
        else -> solarKwh
    }
    val resolvedTariff: Double? = patch.tariffEurPerKwh ?: tariffEurPerKwh
    val recomputedCost: Double? = energyKwh?.let {
        EfficiencyCalc.costEurNetOfSolar(it, solarKwh = resolvedSolarKwh, tariffEurPerKwh = resolvedTariff)
    }
    return copy(
        tariffEurPerKwh = resolvedTariff,
        solarKwh = resolvedSolarKwh,
        costEur = recomputedCost,
    )
}

private fun List<ChargingSession>.toCsv(): String {
    val header = listOf(
        "id", "vehicle_vin", "started_at", "ended_at",
        "start_soc_pct", "end_soc_pct",
        "energy_kwh", "avg_power_kw", "peak_power_kw",
        "connection_type",
        "tariff_eur_kwh", "solar_kwh", "cost_eur",
        "location_lat", "location_lon", "location_label",
    )
    val sb = StringBuilder()
    sb.append(header.joinToString(",")).append('\n')
    forEach { s ->
        sb.append(
            listOf(
                s.id,
                s.vehicleVin,
                s.startedAt.toString(),
                s.endedAt?.toString().orEmpty(),
                s.startSocPct?.toString().orEmpty(),
                s.endSocPct?.toString().orEmpty(),
                s.energyKwh?.toString().orEmpty(),
                s.avgPowerKw?.toString().orEmpty(),
                s.peakPowerKw?.toString().orEmpty(),
                s.connectionType.name,
                s.tariffEurPerKwh?.toString().orEmpty(),
                s.solarKwh?.toString().orEmpty(),
                s.costEur?.toString().orEmpty(),
                s.location?.lat?.toString().orEmpty(),
                s.location?.lon?.toString().orEmpty(),
                s.locationLabel?.let { "\"${it.replace("\"", "\"\"")}\"" }.orEmpty(),
            ).joinToString(","),
        ).append('\n')
    }
    return sb.toString()
}
