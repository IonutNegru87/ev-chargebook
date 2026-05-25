package io.github.inegru.chargebook.backend

import io.github.inegru.chargebook.backend.analytics.analyticsModule
import io.github.inegru.chargebook.backend.auth.authModule
import io.github.inegru.chargebook.backend.auth.authRoutes
import io.github.inegru.chargebook.backend.config.Env
import io.github.inegru.chargebook.backend.persistence.Database
import io.github.inegru.chargebook.backend.persistence.persistenceModule
import io.github.inegru.chargebook.backend.poller.pollerModule
import io.github.inegru.chargebook.backend.routes.analyticsRoutes
import io.github.inegru.chargebook.backend.routes.liveRoutes
import io.github.inegru.chargebook.backend.routes.sessionRoutes
import io.github.inegru.chargebook.backend.volvo.volvoModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.module() {
    val env = Env.fromConfig(environment.config)
    Database.init(env.database)

    install(Koin) {
        slf4jLogger()
        modules(
            envModule(env),
            authModule,
            volvoModule,
            persistenceModule,
            analyticsModule,
            pollerModule,
        )
    }

    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(SSE)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                "Internal error: ${cause.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }

    routing {
        authRoutes(env.volvo)
        sessionRoutes()
        liveRoutes()
        analyticsRoutes()
    }
}

private fun envModule(env: Env) = module {
    single { env }
    single { env.volvo }
    single { env.database }
}
