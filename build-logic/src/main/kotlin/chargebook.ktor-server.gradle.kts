import org.gradle.accessors.dm.LibrariesForLibs

/**
 * Convention plugin for the Ktor (JVM) server module.
 *
 * Bundles the Ktor server + client deps, serialization, Exposed/Hikari/Flyway,
 * Koin, and logging. Consumers add their own `application { mainClass = … }` and
 * any module-specific deps.
 */

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.ktor.plugin")
    id("application")
}

val libs = the<LibrariesForLibs>()

kotlin {
    jvmToolchain(21)
}

dependencies {
    add("implementation", libs.kotlinx.coroutines.core)
    add("implementation", libs.kotlinx.serialization.json)
    add("implementation", libs.kotlinx.datetime)

    add("implementation", libs.ktor.server.core)
    add("implementation", libs.ktor.server.netty)
    add("implementation", libs.ktor.server.content.negotiation)
    add("implementation", libs.ktor.server.status.pages)
    add("implementation", libs.ktor.server.call.logging)
    add("implementation", libs.ktor.server.sse)
    add("implementation", libs.ktor.server.config.yaml)
    add("implementation", libs.ktor.serialization.kotlinx.json)

    add("implementation", libs.ktor.client.core)
    add("implementation", libs.ktor.client.cio)
    add("implementation", libs.ktor.client.content.negotiation)
    add("implementation", libs.ktor.client.logging)
    add("implementation", libs.ktor.client.auth)

    add("implementation", libs.exposed.core)
    add("implementation", libs.exposed.jdbc)
    add("implementation", libs.exposed.kotlin.datetime)
    add("implementation", libs.exposed.json)

    add("implementation", libs.postgres.jdbc)
    add("implementation", libs.hikari)
    add("implementation", libs.flyway.core)
    add("runtimeOnly", libs.flyway.postgres)

    add("implementation", libs.koin.core)
    add("implementation", libs.koin.ktor)
    add("implementation", libs.koin.logger.slf4j)

    add("implementation", libs.logback.classic)

    add("testImplementation", kotlin("test"))
}
