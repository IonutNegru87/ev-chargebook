package io.github.inegru.chargebook.backend

import io.github.inegru.chargebook.backend.analytics.AnalyticsService
import io.github.inegru.chargebook.backend.analytics.analyticsModule
import io.github.inegru.chargebook.backend.auth.AuthRequiredException
import io.github.inegru.chargebook.backend.auth.OAuthStateStore
import io.github.inegru.chargebook.backend.auth.TokenStore
import io.github.inegru.chargebook.backend.auth.VolvoOAuthClient
import io.github.inegru.chargebook.backend.auth.authModule
import io.github.inegru.chargebook.backend.auth.authRoutes
import io.github.inegru.chargebook.backend.config.Env
import io.github.inegru.chargebook.backend.persistence.Database
import io.github.inegru.chargebook.backend.persistence.SessionLocalDataSource
import io.github.inegru.chargebook.backend.persistence.SnapshotLocalDataSource
import io.github.inegru.chargebook.backend.persistence.persistenceModule
import io.github.inegru.chargebook.backend.poller.Poller
import io.github.inegru.chargebook.backend.poller.SnapshotBus
import io.github.inegru.chargebook.backend.poller.pollerModule
import io.github.inegru.chargebook.backend.routes.analyticsRoutes
import io.github.inegru.chargebook.backend.routes.liveRoutes
import io.github.inegru.chargebook.backend.routes.sessionRoutes
import io.github.inegru.chargebook.backend.routes.vehicleRoutes
import io.github.inegru.chargebook.backend.volvo.volvoModule
import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import io.github.inegru.chargebook.shared.data.VolvoVehiclesDataSource
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
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Application")

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
        exception<AuthRequiredException> { call, cause ->
            call.respondText(
                "Not authenticated. Open /auth/start in a browser to sign in. (${cause.message})",
                status = HttpStatusCode.Unauthorized,
            )
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception on ${call.request.local.uri}", cause)
            call.respondText(
                "Internal error: ${cause.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }

    val oauth: VolvoOAuthClient by inject()
    val oauthState: OAuthStateStore by inject()
    val tokenStore: TokenStore by inject()
    val energy: VolvoEnergyDataSource by inject()
    val vehicles: VolvoVehiclesDataSource by inject()
    val snapshotStore: SnapshotLocalDataSource by inject()
    val sessionStore: SessionLocalDataSource by inject()
    val snapshotBus: SnapshotBus by inject()
    val analytics: AnalyticsService by inject()

    routing {
        authRoutes(oauth, oauthState, tokenStore)
        vehicleRoutes(vehicles, energy, snapshotStore)
        sessionRoutes(sessionStore, snapshotStore)
        liveRoutes(snapshotBus)
        analyticsRoutes(analytics)
    }

    val poller: Poller by inject()
    poller.start(this)
}

private fun envModule(env: Env) = module {
    single { env }
    single { env.volvo }
    single { env.database }
    single { env.pricing }
}
