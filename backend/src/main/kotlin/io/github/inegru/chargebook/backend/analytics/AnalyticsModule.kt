package io.github.inegru.chargebook.backend.analytics

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val analyticsModule = module {
    singleOf(::AnalyticsService)
}
