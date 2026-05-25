package io.github.inegru.chargebook.backend.http

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.result.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory

@PublishedApi
internal val safeCallLog = LoggerFactory.getLogger("VolvoHttpClient.safeCall")

/**
 * Safe-call helpers — see the **android-error-handling** skill. Adapted for the
 * JVM backend: catches `UnknownHostException` (the JVM equivalent of
 * `UnresolvedAddressException` on Android) and Ktor's `JsonConvertException`.
 *
 * Callers pass paths that are relative to the base URL configured on the
 * `HttpClient` via [HttpClientFactory], so there is no `BuildConfig.BASE_URL`
 * sibling here.
 */

suspend inline fun <reified Response : Any> HttpClient.safeGet(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
    crossinline configure: HttpRequestBuilder.() -> Unit = {},
): Result<Response, DataError.Network> = safeCall {
    get(route) {
        queryParameters.forEach { (key, value) -> parameter(key, value) }
        configure()
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse,
): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: UnknownHostException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: JsonConvertException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Network> {
    val code = response.status.value
    if (code in 200..299) {
        return Result.Success(response.body<T>())
    }
    // Read the error body once and log it before returning the typed error —
    // Ktor's logging plugin doesn't capture bodies for non-2xx responses.
    val body = runCatching { response.bodyAsText() }.getOrDefault("")
    safeCallLog.warn(
        "Upstream {} from {}: {}",
        code,
        response.call.request.url,
        body.take(500),
    )
    return Result.Error(
        when (code) {
            400 -> DataError.Network.BAD_REQUEST
            401 -> DataError.Network.UNAUTHORIZED
            403 -> DataError.Network.FORBIDDEN
            404 -> DataError.Network.NOT_FOUND
            408 -> DataError.Network.REQUEST_TIMEOUT
            409 -> DataError.Network.CONFLICT
            413 -> DataError.Network.PAYLOAD_TOO_LARGE
            429 -> DataError.Network.TOO_MANY_REQUESTS
            503 -> DataError.Network.SERVICE_UNAVAILABLE
            in 500..599 -> DataError.Network.SERVER_ERROR
            else -> DataError.Network.UNKNOWN
        }
    )
}
