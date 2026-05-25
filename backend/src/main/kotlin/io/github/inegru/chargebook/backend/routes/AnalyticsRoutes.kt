package io.github.inegru.chargebook.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.analyticsRoutes() {
    route("/api/analytics") {
        get("/monthly") {
            // TODO: AnalyticsService.monthly(vin, from, to)
            call.respondText("not implemented", status = HttpStatusCode.NotImplemented)
        }
    }
}
