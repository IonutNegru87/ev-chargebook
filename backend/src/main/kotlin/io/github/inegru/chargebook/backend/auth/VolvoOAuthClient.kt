package io.github.inegru.chargebook.backend.auth

import io.github.inegru.chargebook.backend.config.VolvoConfig
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant,
    val scope: String? = null,
)

data class PkcePair(val codeVerifier: String, val codeChallenge: String)

/**
 * OAuth 2.0 Authorization Code Flow with PKCE for the Volvo Developer Portal.
 *
 * See https://developer.volvocars.com/apis/docs/authorisation/.
 */
class VolvoOAuthClient(private val config: VolvoConfig) {

    fun buildAuthorizeUrl(state: String, pkce: PkcePair, scopes: List<String>): String {
        TODO("Compose authorize URL with client_id, redirect_uri, response_type=code, scope, state, code_challenge, code_challenge_method=S256")
    }

    fun generatePkcePair(): PkcePair {
        TODO("Generate code_verifier (43-128 chars) and SHA-256 code_challenge")
    }

    suspend fun exchangeCode(code: String, codeVerifier: String): TokenResponse {
        TODO("POST tokenUrl with grant_type=authorization_code, code, redirect_uri, code_verifier, client_id, client_secret")
    }

    suspend fun refresh(refreshToken: String): TokenResponse {
        TODO("POST tokenUrl with grant_type=refresh_token")
    }
}
