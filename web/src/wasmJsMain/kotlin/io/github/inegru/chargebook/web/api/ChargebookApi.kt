package io.github.inegru.chargebook.web.api

import io.github.inegru.chargebook.shared.analytics.MonthlyTotals
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * Browser-side typed client for the chargebook backend. Hits the same
 * `:shared` domain types the backend returns, so there's no separate web DTO
 * layer.
 *
 * `baseUrl` defaults to the dev backend (`localhost:8080`). When the web bundle
 * is served from the same origin as the API (production), switch this to an
 * empty string.
 */
class ChargebookApi(
    val baseUrl: String = "http://localhost:8080",
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val signInUrl: String get() = "$baseUrl/auth/start"

    val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(SSE)
    }

    sealed interface SnapshotResult {
        data class Success(val snapshot: ChargingSnapshot) : SnapshotResult
        data object Unauthorized : SnapshotResult
        data object NotFound : SnapshotResult
        data class Error(val message: String) : SnapshotResult
    }

    suspend fun latestSnapshot(): SnapshotResult = try {
        val response: HttpResponse = httpClient.get("$baseUrl/api/snapshot/latest")
        when (response.status) {
            HttpStatusCode.OK -> SnapshotResult.Success(response.body())
            HttpStatusCode.Unauthorized -> SnapshotResult.Unauthorized
            HttpStatusCode.NotFound -> SnapshotResult.NotFound
            else -> SnapshotResult.Error("HTTP ${response.status.value}")
        }
    } catch (e: Throwable) {
        SnapshotResult.Error(e.message ?: e::class.simpleName.orEmpty())
    }

    sealed interface ListResult<out T> {
        data class Success<T>(val data: T) : ListResult<T>
        data object Unauthorized : ListResult<Nothing>
        data class Error(val message: String) : ListResult<Nothing>
    }

    suspend fun sessions(): ListResult<List<ChargingSession>> =
        getList("$baseUrl/api/sessions")

    suspend fun monthlyTotals(): ListResult<List<MonthlyTotals>> =
        getList("$baseUrl/api/analytics/monthly")

    private suspend inline fun <reified T> getList(url: String): ListResult<T> = try {
        val response: HttpResponse = httpClient.get(url)
        when (response.status) {
            HttpStatusCode.OK -> ListResult.Success(response.body<T>())
            HttpStatusCode.Unauthorized -> ListResult.Unauthorized
            else -> ListResult.Error("HTTP ${response.status.value}")
        }
    } catch (e: Throwable) {
        ListResult.Error(e.message ?: e::class.simpleName.orEmpty())
    }

    /**
     * Server-Sent Events stream of snapshots from `/api/live`. The backend
     * replays the latest known snapshot on connect, so we get an initial value
     * without polling.
     */
    fun liveSnapshots(): Flow<ChargingSnapshot> = flow {
        httpClient.sse(urlString = "$baseUrl/api/live") {
            incoming.collect { event ->
                val data = event.data ?: return@collect
                val parsed = runCatching { json.decodeFromString<ChargingSnapshot>(data) }.getOrNull()
                if (parsed != null) emit(parsed)
            }
        }
    }
}
