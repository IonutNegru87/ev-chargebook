plugins {
    id("chargebook.cmp-web")
}

kotlin {
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}
