import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention plugin for pure-Kotlin / KMP "core domain" modules.
 *
 * Models the `:core:domain` role from the team's Android architecture: pure Kotlin,
 * no platform or framework deps, depends on nothing else. `:shared` consumes this
 * today; future Android targets can be added by appending to `kotlin { … }` in the
 * module's own build script.
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = the<LibrariesForLibs>()

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
