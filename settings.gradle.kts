rootProject.name = "ev-chargebook"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    // No `repositoriesMode` set — the Kotlin/Wasm toolchain plugin registers
    // its own nodejs distribution repository at the project level, and any
    // mode that warns on project-level repos rejects it.
    repositories {
        mavenCentral()
        google()
    }
}

include(":shared")
include(":backend")
include(":web")
