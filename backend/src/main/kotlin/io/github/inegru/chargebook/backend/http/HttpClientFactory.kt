package io.github.inegru.chargebook.backend.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Builds the Ktor [HttpClient] used to talk to the Volvo Energy API.
 *
 * The engine is injected so tests can supply a `MockEngine`. Authorization is
 * deliberately not configured here — callers attach a fresh `Bearer` per request
 * via the token provider passed to the data source, which keeps refresh logic in
 * one place.
 */
object HttpClientFactory {

    fun create(
        baseUrl: String,
        vccApiKey: String,
        engine: HttpClientEngine = CIO.create(),
    ): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(Logging) {
            val slf4j = LoggerFactory.getLogger("VolvoHttpClient")
            logger = object : Logger {
                override fun log(message: String) = slf4j.info(message)
            }
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(baseUrl)
            header("vcc-api-key", vccApiKey)
            contentType(ContentType.Application.Json)
        }
    }
}
