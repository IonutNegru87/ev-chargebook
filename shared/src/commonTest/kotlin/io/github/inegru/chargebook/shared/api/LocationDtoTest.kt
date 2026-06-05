package io.github.inegru.chargebook.shared.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LocationDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun parses_volvo_geojson_response() {
        // Verbatim payload captured from /location/v1/vehicles/{vin}/location.
        val payload = """
            {
              "status": 200,
              "operationId": "b19660fad40a4011a15ecd5c068ce081",
              "data": {
                "type": "Feature",
                "properties": {
                  "heading": "0",
                  "timestamp": "2026-06-04T05:27:31.811Z"
                },
                "geometry": {
                  "type": "Point",
                  "coordinates": [27.51681111111111, 47.186790277777774, 0.0]
                }
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString<LocationDto>(payload)
        val coords = dto.data?.geometry?.coordinates
        assertNotNull(coords)
        assertEquals(27.51681111111111, coords[0])  // longitude (GeoJSON ordering)
        assertEquals(47.186790277777774, coords[1]) // latitude
    }
}
