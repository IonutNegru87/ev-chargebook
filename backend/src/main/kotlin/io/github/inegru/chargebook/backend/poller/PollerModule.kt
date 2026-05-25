package io.github.inegru.chargebook.backend.poller

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val pollerModule = module {
    singleOf(::SessionDetector)
    // Lambda overload: Poller has default-valued Duration params that singleOf
    // would otherwise try to resolve from the container.
    single { Poller(vehicles = get(), energy = get(), snapshots = get()) }
}
