package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.backend.auth.AccessTokenProvider
import io.github.inegru.chargebook.backend.config.VolvoConfig
import io.github.inegru.chargebook.backend.http.HttpClientFactory
import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import io.github.inegru.chargebook.shared.data.VolvoVehiclesDataSource
import org.koin.dsl.bind
import org.koin.dsl.module

val volvoModule = module {
    // Lambda overload: HttpClient is built via a factory method, not a constructor.
    single {
        val config: VolvoConfig = get()
        HttpClientFactory.create(
            baseUrl = config.apiBaseUrl,
            vccApiKey = config.vccApiKey,
        )
    }

    single {
        val tokens: AccessTokenProvider = get()
        KtorVolvoEnergyDataSource(
            httpClient = get(),
            tokenProvider = { tokens(DEFAULT_USER) },
        )
    } bind VolvoEnergyDataSource::class

    single {
        val tokens: AccessTokenProvider = get()
        KtorVolvoVehiclesDataSource(
            httpClient = get(),
            tokenProvider = { tokens(DEFAULT_USER) },
        )
    } bind VolvoVehiclesDataSource::class
}

/** Single-tenant default until per-user auth lands. */
const val DEFAULT_USER: String = "default"
