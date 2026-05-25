package io.github.inegru.chargebook.backend.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent

fun Route.liveRoutes() {
    sse("/api/live") {
        // TODO: emit ChargingSnapshot updates as they arrive from the poller
        send(ServerSentEvent(data = "connected", event = "ready"))
    }
}

@Suppress("unused")
private suspend fun ServerSSESession.placeholder() = Unit
