package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.Serializable

/**
 * A session plus every snapshot tagged with its id, ordered by `recordedAt`.
 * Returned by `GET /api/sessions/{id}` and consumed by the web session-detail
 * screen, so it lives in `:shared`.
 */
@Serializable
data class SessionWithSnapshots(
    val session: ChargingSession,
    val snapshots: List<ChargingSnapshot>,
)
