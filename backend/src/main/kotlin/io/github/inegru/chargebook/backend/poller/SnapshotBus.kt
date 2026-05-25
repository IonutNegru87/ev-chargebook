package io.github.inegru.chargebook.backend.poller

import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fan-out of newly persisted snapshots. The poller emits; SSE subscribers
 * collect.
 *
 * `replay = 1` so a new SSE client immediately gets the latest known state
 * without having to wait for the next poll tick (which could be up to 30 min
 * away when the car is disconnected). `DROP_OLDEST` keeps the emitter
 * non-suspending if a subscriber is slow.
 */
class SnapshotBus {
    private val _flow = MutableSharedFlow<ChargingSnapshot>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val flow: SharedFlow<ChargingSnapshot> = _flow.asSharedFlow()

    fun publish(snapshot: ChargingSnapshot) {
        _flow.tryEmit(snapshot)
    }
}
