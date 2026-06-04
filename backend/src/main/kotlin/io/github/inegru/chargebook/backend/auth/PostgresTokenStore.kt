package io.github.inegru.chargebook.backend.auth

import io.github.inegru.chargebook.backend.persistence.OAuthTokenTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import org.slf4j.LoggerFactory

/**
 * Persistent [TokenStore] backed by Postgres. Survives backend restarts, which
 * lets the poller pick up where it left off instead of waiting for a manual
 * re-sign-in.
 */
class PostgresTokenStore : TokenStore {

    private val log = LoggerFactory.getLogger("PostgresTokenStore")

    override suspend fun get(userId: String): StoredToken? = newSuspendedTransaction {
        OAuthTokenTable
            .selectAll()
            .where { OAuthTokenTable.userId eq userId }
            .firstOrNull()
            ?.let { row ->
                StoredToken(
                    vehicleVin = row[OAuthTokenTable.vehicleVin],
                    accessToken = row[OAuthTokenTable.accessToken],
                    refreshToken = row[OAuthTokenTable.refreshToken],
                    expiresAt = row[OAuthTokenTable.expiresAt],
                )
            }
    }

    override suspend fun put(userId: String, token: StoredToken) {
        newSuspendedTransaction {
            OAuthTokenTable.upsert(OAuthTokenTable.userId) {
                it[OAuthTokenTable.userId] = userId
                it[vehicleVin] = token.vehicleVin
                it[accessToken] = token.accessToken
                it[refreshToken] = token.refreshToken
                it[expiresAt] = token.expiresAt
                it[updatedAt] = Clock.System.now()
            }
        }
        log.info("Stored OAuth token for '$userId' (expires {})", token.expiresAt)
    }

    override suspend fun delete(userId: String) {
        newSuspendedTransaction {
            OAuthTokenTable.deleteWhere { OAuthTokenTable.userId eq userId }
        }
    }
}
