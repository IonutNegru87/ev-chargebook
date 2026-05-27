package io.github.inegru.chargebook.shared.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargingSnapshotSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun deserializes_target_soc_from_backend_payload() {
        // Verbatim body returned by GET /api/snapshot/latest.
        val payload =
            """{"recordedAt":"2026-05-27T08:15:02Z","vehicleVin":"YV12ZEM18T2608642","sessionId":null,"socPct":86,"targetSocPct":100,"powerKw":null,"rangeKm":218,"estimatedMinutes":211,"chargingStatus":"IDLE","connectionStatus":"DISCONNECTED"}"""

        val snapshot = json.decodeFromString<ChargingSnapshot>(payload)

        assertEquals(100, snapshot.targetSocPct)
        assertEquals(86, snapshot.socPct)
        assertEquals(211, snapshot.estimatedMinutes)
    }

    @Test
    fun target_label_reflects_value() {
        // Mirrors the dashboard label rule.
        fun label(target: Int?) = target?.let { "time to $it%" } ?: "time to full"

        assertEquals("time to 100%", label(100))
        assertEquals("time to 80%", label(80))
        assertEquals("time to full", label(null))
    }
}
