package io.github.inegru.chargebook.backend.persistence

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val persistenceModule = module {
    singleOf(::ExposedSessionDataSource) { bind<SessionLocalDataSource>() }
    singleOf(::ExposedSnapshotDataSource) { bind<SnapshotLocalDataSource>() }
}
