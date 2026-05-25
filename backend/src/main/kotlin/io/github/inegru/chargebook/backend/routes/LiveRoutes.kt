package io.github.inegru.chargebook.backend.routes

import io.github.inegru.chargebook.backend.poller.SnapshotBus
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.ktor.server.routing.Route
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Server-Sent Events stream of newly persisted snapshots. The bus replays the
 * latest known snapshot on connect, so a fresh client doesn't have to wait for
 * the next poll tick to render initial state.
 */
fun Route.liveRoutes(bus: SnapshotBus) {
    val json = Json { encodeDefaults = true }
    sse("/api/live") {
        bus.flow.collectLatest { snapshot: ChargingSnapshot ->
            send(
                ServerSentEvent(
                    data = json.encodeToString(snapshot),
                    event = "snapshot",
                ),
            )
        }
    }
}
