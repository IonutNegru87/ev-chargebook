package io.github.inegru.chargebook.backend.auth

import java.util.concurrent.ConcurrentHashMap
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Short-lived per-OAuth-attempt context, keyed by the `state` value sent to the
 * authorize URL. We hold onto the PKCE code_verifier so we can present it on the
 * token-exchange call.
 *
 * In-memory and single-process: fine for a single-user dev backend. If we ever
 * shard the backend, this needs to move to Redis or signed cookies.
 */
class OAuthStateStore(private val ttl: Duration = 10.minutes) {

    data class Entry(val verifier: String, val createdAt: Instant)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(state: String, verifier: String) {
        prune()
        entries[state] = Entry(verifier, Clock.System.now())
    }

    /** Returns and removes the entry — state values are single-use. */
    fun consume(state: String): Entry? {
        prune()
        return entries.remove(state)
    }

    private fun prune() {
        val cutoff = Clock.System.now() - ttl
        entries.entries.removeIf { it.value.createdAt < cutoff }
    }
}
