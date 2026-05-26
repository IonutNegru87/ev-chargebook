package io.github.inegru.chargebook.backend.config

import io.ktor.server.config.ApplicationConfig

data class VolvoConfig(
    val clientId: String,
    val clientSecret: String,
    val vccApiKey: String,
    val redirectUri: String,
    val authorizeUrl: String,
    val tokenUrl: String,
    val apiBaseUrl: String,
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

data class PricingConfig(
    /** €/kWh applied automatically at session close. `null` disables auto-pricing. */
    val defaultTariffEurPerKwh: Double?,
)

data class WebConfig(
    /** Where `/auth/callback` redirects after successful sign-in. */
    val appBaseUrl: String,
)

data class Env(
    val volvo: VolvoConfig,
    val database: DatabaseConfig,
    val pricing: PricingConfig,
    val web: WebConfig,
) {
    companion object {
        fun fromConfig(config: ApplicationConfig): Env {
            val volvo = config.config("chargebook.volvo")
            val db = config.config("chargebook.database")
            val pricing = config.config("chargebook.pricing")
            val web = config.config("chargebook.web")
            return Env(
                volvo = VolvoConfig(
                    clientId = volvo.tryGetString("clientId").orEmpty(),
                    clientSecret = volvo.tryGetString("clientSecret").orEmpty(),
                    vccApiKey = volvo.tryGetString("vccApiKey").orEmpty(),
                    redirectUri = volvo.property("redirectUri").getString(),
                    authorizeUrl = volvo.property("authorizeUrl").getString(),
                    tokenUrl = volvo.property("tokenUrl").getString(),
                    apiBaseUrl = volvo.property("apiBaseUrl").getString(),
                ),
                database = DatabaseConfig(
                    url = db.property("url").getString(),
                    user = db.property("user").getString(),
                    password = db.property("password").getString(),
                ),
                pricing = PricingConfig(
                    defaultTariffEurPerKwh = pricing.tryGetString("defaultTariffEurPerKwh")?.toDoubleOrNull(),
                ),
                web = WebConfig(
                    appBaseUrl = web.property("appBaseUrl").getString(),
                ),
            )
        }
    }
}

private fun ApplicationConfig.tryGetString(key: String): String? =
    runCatching { property(key).getString() }.getOrNull()
