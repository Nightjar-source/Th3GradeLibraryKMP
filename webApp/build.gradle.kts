plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("org.jetbrains.compose.runtime:runtime:${libs.versions.compose.get()}")
            implementation("org.jetbrains.compose.foundation:foundation:${libs.versions.compose.get()}")
            implementation("org.jetbrains.compose.material3:material3:${libs.versions.compose.get()}")
            implementation("org.jetbrains.compose.ui:ui:${libs.versions.compose.get()}")
        }
    }
}
