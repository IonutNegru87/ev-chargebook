package io.github.inegru.chargebook.backend.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.inegru.chargebook.backend.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database as ExposedDatabase

object Database {

    fun init(config: DatabaseConfig): ExposedDatabase {
        val hikari = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        })

        Flyway.configure()
            .dataSource(hikari)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        return ExposedDatabase.connect(hikari)
    }
}
