package io.github.inegru.chargebook.backend.location

import io.github.inegru.chargebook.shared.model.GeoPoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.math.round

/**
 * Reverse-geocodes via Nominatim (OpenStreetMap). Free, but with a strict
 * usage policy: max 1 request/second, must send a real User-Agent, and
 * results should be cached.
 *
 * Cache key is lat/lon rounded to 3 decimal places (~110 m grid) — "still at
 * the same parking spot" hits the cache, a meaningful move hits Nominatim.
 */
class NominatimLocationLabeler(
    private val userAgent: String = "ev-chargebook/0.1 (github.com/IonutNegru87/ev-chargebook)",
    engine: HttpClientEngine = CIO.create(),
) : LocationLabeler {

    private val log = LoggerFactory.getLogger("NominatimLocationLabeler")
    private val cache = ConcurrentHashMap<String, String?>()
    private val rateLimit = Mutex()
    private var lastCallAtMillis: Long = 0L

    private val httpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        defaultRequest {
            url("https://nominatim.openstreetmap.org/")
            header(HttpHeaders.UserAgent, userAgent)
        }
    }

    override suspend fun label(point: GeoPoint): String? {
        val key = roundKey(point)
        cache[key]?.let { return it }
        if (cache.containsKey(key)) return null // negative cache hit

        val label = try {
            rateLimit.withLock {
                throttle()
                fetch(point)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Nominatim lookup failed for {}: {}", key, e.message)
            null
        }
        cache[key] = label
        return label
    }

    private suspend fun throttle() {
        // Nominatim usage policy: max 1 req/sec.
        val now = System.currentTimeMillis()
        val sinceLast = now - lastCallAtMillis
        if (sinceLast in 0..999) delay(1_000L - sinceLast)
        lastCallAtMillis = System.currentTimeMillis()
    }

    private suspend fun fetch(point: GeoPoint): String? {
        val response: HttpResponse = httpClient.get("reverse") {
            parameter("format", "jsonv2")
            parameter("lat", point.lat)
            parameter("lon", point.lon)
            parameter("zoom", 14)
        }
        if (response.status.value !in 200..299) {
            log.warn("Nominatim {}: {}", response.status, response.bodyAsTextSafe())
            return null
        }
        val dto: NominatimReverseDto = response.body()
        return dto.toLabel()
    }

    private suspend fun HttpResponse.bodyAsTextSafe(): String =
        runCatching { body<String>() }.getOrDefault("<no body>")

    private fun roundKey(p: GeoPoint): String {
        val rl = round(p.lat * 1000) / 1000
        val ro = round(p.lon * 1000) / 1000
        return "$rl,$ro"
    }
}

@Serializable
private data class NominatimReverseDto(
    @SerialName("display_name") val displayName: String? = null,
    val address: NominatimAddressDto? = null,
)

@Serializable
private data class NominatimAddressDto(
    val road: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val suburb: String? = null,
    val country: String? = null,
)

private fun NominatimReverseDto.toLabel(): String? {
    val a = address
    val place = a?.city ?: a?.town ?: a?.village ?: a?.suburb
    val road = a?.road
    return when {
        road != null && place != null -> "$road, $place"
        place != null -> place
        else -> displayName
    }
}
