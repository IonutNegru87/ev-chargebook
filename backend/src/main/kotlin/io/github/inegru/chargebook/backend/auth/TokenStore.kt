package io.github.inegru.chargebook.backend.auth

import kotlinx.datetime.Instant

data class StoredToken(
    val vehicleVin: String?,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant,
)

/**
 * Persists OAuth tokens. Real implementation should encrypt at rest — Volvo refresh
 * tokens are long-lived and grant access to vehicle data.
 */
interface TokenStore {
    suspend fun get(userId: String): StoredToken?
    suspend fun put(userId: String, token: StoredToken)
    suspend fun delete(userId: String)
}

class InMemoryTokenStore : TokenStore {
    private val tokens = mutableMapOf<String, StoredToken>()
    override suspend fun get(userId: String): StoredToken? = tokens[userId]
    override suspend fun put(userId: String, token: StoredToken) { tokens[userId] = token }
    override suspend fun delete(userId: String) { tokens.remove(userId) }
}
