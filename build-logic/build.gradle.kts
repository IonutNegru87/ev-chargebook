plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.kotlin.compose.compiler.plugin)
    implementation(libs.ktor.gradle.plugin)
    implementation(libs.compose.multiplatform.plugin)

    // Exposes the `libs` type-safe accessor inside precompiled script plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
