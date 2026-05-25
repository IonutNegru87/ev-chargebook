package io.github.inegru.chargebook.backend.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Reads an access token from [TokenStore], refreshing via [VolvoOAuthClient]
 * if it's within [skew] of expiry.
 *
 * Serialized via [mutex] so concurrent callers can't fire duplicate refresh
 * requests (which would each issue a new refresh token and invalidate the
 * other).
 */
class AccessTokenProvider(
    private val tokenStore: TokenStore,
    private val oauth: VolvoOAuthClient,
) {
    private val mutex = Mutex()
    private val skew = 60.seconds

    suspend operator fun invoke(userId: String): String = mutex.withLock {
        val current = tokenStore.get(userId) ?: throw AuthRequiredException("No token for '$userId'")
        if (Clock.System.now() < current.expiresAt - skew) {
            return@withLock current.accessToken
        }
        val refresh = current.refreshToken
            ?: throw AuthRequiredException("No refresh token stored; user must re-authenticate")
        val fresh = oauth.refresh(refresh)
        val updated = StoredToken(
            vehicleVin = current.vehicleVin,
            accessToken = fresh.accessToken,
            refreshToken = fresh.refreshToken ?: refresh,
            expiresAt = fresh.expiresAt,
        )
        tokenStore.put(userId, updated)
        updated.accessToken
    }
}

class AuthRequiredException(message: String) : RuntimeException(message)
