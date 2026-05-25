package io.github.inegru.chargebook.backend.poller

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val pollerModule = module {
    // Lambda overloads: both classes have default-valued primary-constructor
    // params (Int debounce, Duration retry intervals) that singleOf would
    // otherwise try to resolve from the container.
    single { SessionDetector() }
    single { SnapshotBus() }
    single {
        Poller(
            vehicles = get(),
            energy = get(),
            snapshots = get(),
            sessions = get(),
            sessionDetector = get(),
            snapshotBus = get(),
        )
    }
}
