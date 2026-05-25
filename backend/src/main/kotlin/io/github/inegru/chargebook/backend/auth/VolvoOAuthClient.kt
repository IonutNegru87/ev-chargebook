package io.github.inegru.chargebook.backend.auth

import io.github.inegru.chargebook.backend.config.VolvoConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * OAuth 2.0 Authorization Code Flow with PKCE for the Volvo Developer Portal.
 *
 * See https://developer.volvocars.com/apis/docs/authorisation/.
 */
class VolvoOAuthClient(
    private val config: VolvoConfig,
    engine: HttpClientEngine = CIO.create(),
) {
    private val http = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun buildAuthorizeUrl(state: String, codeChallenge: String, scopes: List<String>): String =
        URLBuilder(config.authorizeUrl).apply {
            parameters.append("response_type", "code")
            parameters.append("client_id", config.clientId)
            parameters.append("redirect_uri", config.redirectUri)
            parameters.append("scope", scopes.joinToString(" "))
            parameters.append("state", state)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
        }.buildString()

    suspend fun exchangeCode(code: String, codeVerifier: String): TokenResponse =
        postToken(
            parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", config.redirectUri)
                append("code_verifier", codeVerifier)
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
            }
        )

    suspend fun refresh(refreshToken: String): TokenResponse =
        postToken(
            parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
            }
        )

    private suspend fun postToken(form: Parameters): TokenResponse {
        val raw: RawTokenResponse = http.submitForm(
            url = config.tokenUrl,
            formParameters = form,
        ).body()
        return raw.toDomain()
    }

    @Serializable
    private data class RawTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long,
        @SerialName("scope") val scope: String? = null,
        @SerialName("token_type") val tokenType: String? = null,
        @SerialName("id_token") val idToken: String? = null,
    )

    private fun RawTokenResponse.toDomain(): TokenResponse = TokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAt = Clock.System.now() + expiresIn.seconds,
        scope = scope,
    )
}

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant,
    val scope: String?,
)

/** Scopes we request — keep in sync with what the Volvo portal app has published. */
val DEFAULT_SCOPES: List<String> = listOf(
    "openid",
    "energy:capability:read",
    "energy:state:read",
    "conve:vehicle_relation",
    "location:read",
)
