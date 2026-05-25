package io.github.inegru.chargebook.backend.auth

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule = module {
    // Lambda overload: VolvoOAuthClient has a default-valued engine param
    // that singleOf would otherwise try to resolve from the container.
    single { VolvoOAuthClient(config = get()) }
    single { OAuthStateStore() }
    singleOf(::AccessTokenProvider)
    singleOf(::InMemoryTokenStore) { bind<TokenStore>() }
}
