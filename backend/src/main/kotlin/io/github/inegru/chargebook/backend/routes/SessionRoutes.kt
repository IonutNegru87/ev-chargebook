package io.github.inegru.chargebook.backend.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.sessionRoutes() {
    route("/api/sessions") {
        get {
            // TODO: list sessions, accept ?vin=&since=&limit=
            call.respondText("[]")
        }
        get("/{id}") {
            // TODO: fetch single session with its snapshots
            call.respondText("not implemented", status = HttpStatusCode.NotImplemented)
        }
    }
}
