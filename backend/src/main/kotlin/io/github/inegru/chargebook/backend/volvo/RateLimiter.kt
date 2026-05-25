package io.github.inegru.chargebook.backend.volvo

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Soft client-side rate limiter mirroring the documented quotas:
 * - 100 requests/minute per (user × client_id)
 * - 10,000 requests/day per API key
 *
 * Keeps a sliding window in memory. Good enough for single-tenant; multi-tenant
 * deployments should swap this for a Redis-backed limiter and round-robin
 * across multiple API keys.
 */
class RateLimiter(
    private val perMinute: Int = 100,
    private val perDay: Int = 10_000,
) {
    private val mutex = Mutex()
    private val minuteWindow = ArrayDeque<TimeSource.Monotonic.ValueTimeMark>()
    private val dayWindow = ArrayDeque<TimeSource.Monotonic.ValueTimeMark>()

    suspend fun acquire(): Boolean = mutex.withLock {
        val now = TimeSource.Monotonic.markNow()
        prune(minuteWindow, 1.minutes, now)
        prune(dayWindow, Duration.parse("1d"), now)
        if (minuteWindow.size >= perMinute || dayWindow.size >= perDay) return@withLock false
        minuteWindow.addLast(now)
        dayWindow.addLast(now)
        true
    }

    private fun prune(
        window: ArrayDeque<TimeSource.Monotonic.ValueTimeMark>,
        keep: Duration,
        now: TimeSource.Monotonic.ValueTimeMark,
    ) {
        while (window.isNotEmpty() && (now - window.first()) > keep) {
            window.removeFirst()
        }
    }
}
