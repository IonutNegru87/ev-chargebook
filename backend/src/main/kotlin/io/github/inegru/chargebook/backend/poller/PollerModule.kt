package io.github.inegru.chargebook.backend.poller

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val pollerModule = module {
    singleOf(::SessionDetector)
}
