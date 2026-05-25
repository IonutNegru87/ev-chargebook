package io.github.inegru.chargebook.backend.routes

import io.github.inegru.chargebook.backend.analytics.AnalyticsService
import io.github.inegru.chargebook.shared.result.Result
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate

fun Route.analyticsRoutes(analytics: AnalyticsService) {
    route("/api/analytics") {
        get("/monthly") {
            val vin = call.request.queryParameters["vin"]
            val from = call.request.queryParameters["from"]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val to = call.request.queryParameters["to"]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            when (val r = analytics.monthly(vehicleVin = vin, from = from, to = to)) {
                is Result.Error -> call.respondText(
                    "Local store error: ${r.error}",
                    status = HttpStatusCode.InternalServerError,
                )
                is Result.Success -> call.respond(r.data)
            }
        }
    }
}
