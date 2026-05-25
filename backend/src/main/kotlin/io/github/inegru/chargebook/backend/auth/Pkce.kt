package io.github.inegru.chargebook.backend.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PKCE (RFC 7636) helpers — code_verifier is a 43–128 char URL-safe random
 * string; code_challenge is `BASE64URL(SHA256(code_verifier))`.
 */
object Pkce {

    private val random = SecureRandom()
    private val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    /** Generates a 96-char code_verifier (well above the 43-char minimum). */
    fun generateVerifier(): String {
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return urlEncoder.encodeToString(bytes)
    }

    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return urlEncoder.encodeToString(digest)
    }

    /** Opaque CSRF state value mixed into the authorize URL and verified on callback. */
    fun generateState(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return urlEncoder.encodeToString(bytes)
    }
}
