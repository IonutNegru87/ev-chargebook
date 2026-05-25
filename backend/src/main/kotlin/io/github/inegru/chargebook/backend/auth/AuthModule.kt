package io.github.inegru.chargebook.backend.auth

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule = module {
    singleOf(::VolvoOAuthClient)
    singleOf(::InMemoryTokenStore) { bind<TokenStore>() }
}
