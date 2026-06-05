package io.github.inegru.chargebook.backend.location

import org.koin.dsl.bind
import org.koin.dsl.module

val locationModule = module {
    // Lambda form — NominatimLocationLabeler has default-valued constructor
    // params (userAgent, engine) that singleOf would try to resolve.
    single { NominatimLocationLabeler() } bind LocationLabeler::class
}
