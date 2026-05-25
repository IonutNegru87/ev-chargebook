package io.github.inegru.chargebook.backend.volvo

import io.github.inegru.chargebook.backend.auth.TokenStore
import io.github.inegru.chargebook.backend.config.VolvoConfig
import io.github.inegru.chargebook.backend.http.HttpClientFactory
import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import org.koin.dsl.bind
import org.koin.dsl.module

val volvoModule = module {
    // Lambda overload: building HttpClient via a factory method, not a constructor.
    single {
        val config: VolvoConfig = get()
        HttpClientFactory.create(
            baseUrl = config.apiBaseUrl,
            vccApiKey = config.vccApiKey,
        )
    }

    single {
        KtorVolvoEnergyDataSource(
            httpClient = get(),
            // The token provider is a closure so refresh logic can live behind it
            // later without touching the data source.
            tokenProvider = {
                val store: TokenStore = get()
                requireNotNull(store.get(DEFAULT_USER)) { "Not authenticated" }.accessToken
            },
        )
    } bind VolvoEnergyDataSource::class
}

/** Single-tenant default until per-user auth lands. */
const val DEFAULT_USER: String = "default"
