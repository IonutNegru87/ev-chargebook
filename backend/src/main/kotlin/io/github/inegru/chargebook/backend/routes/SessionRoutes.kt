package io.github.inegru.chargebook.backend.routes

import io.github.inegru.chargebook.backend.persistence.SessionLocalDataSource
import io.github.inegru.chargebook.backend.persistence.SnapshotLocalDataSource
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.result.Result
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SessionWithSnapshots(
    val session: ChargingSession,
    val snapshots: List<ChargingSnapshot>,
)

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
    }
}
