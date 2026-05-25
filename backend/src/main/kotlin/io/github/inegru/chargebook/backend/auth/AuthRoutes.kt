package io.github.inegru.chargebook.backend.auth

import io.github.inegru.chargebook.backend.config.VolvoConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.authRoutes(volvo: VolvoConfig) {
    val oauth = VolvoOAuthClient(volvo)

    route("/auth") {
        get("/start") {
            // TODO: generate PKCE + state, store in session, redirect to authorize URL
            call.respondText("auth start not implemented", status = HttpStatusCode.NotImplemented)
        }
        get("/callback") {
            // TODO: validate state, exchange code for tokens, persist via TokenStore
            call.respondText("auth callback not implemented", status = HttpStatusCode.NotImplemented)
        }
    }
}
