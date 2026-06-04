package io.github.inegru.chargebook.backend.routes

import io.github.inegru.chargebook.backend.auth.TokenStore
import io.github.inegru.chargebook.backend.volvo.DEFAULT_USER
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Serializable
data class Health(
    val status: String,
    val db: String,
    val auth: String,
)

/**
 * Liveness + readiness on one endpoint. Returns 200 when both Postgres is
 * reachable and an OAuth token is on file; 503 otherwise. Designed for a
 * single curl from a load balancer or monitoring agent.
 */
fun Route.healthRoutes(tokenStore: TokenStore) {
    get("/health") {
        val dbStatus = runCatching {
            newSuspendedTransaction {
                exec("SELECT 1") { it.next() }
            }
            "ok"
        }.getOrElse { "down: ${it.message}" }

        val authStatus = runCatching {
            if (tokenStore.get(DEFAULT_USER) != null) "ok" else "unauthenticated"
        }.getOrElse { "error: ${it.message}" }

        val overall = if (dbStatus == "ok" && authStatus == "ok") "ok" else "degraded"
        val httpStatus = if (overall == "ok") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(httpStatus, Health(status = overall, db = dbStatus, auth = authStatus))
    }
}
