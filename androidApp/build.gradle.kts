plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val copySharedResources = tasks.register<Copy>("copySharedResources") {
    dependsOn(":shared:prepareComposeResourcesTaskForCommonMain")
    from(project(":shared").file("src/commonMain/composeResources"))
    into(layout.buildDirectory.dir("intermediates/shared_assets/composeResources/com.Nightjar.Th3GradeLibraryKMP.generated.resources"))
}

android {
    namespace = "com.Nightjar.Th3GradeLibraryKMP"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.Nightjar.Th3GradeLibraryKMP"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.32.2"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("Nightjar.keystore")
            storePassword = "19941994"
            keyAlias = "Nightjar"
            keyPassword = "19941994"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isCrunchPngs = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    // ضمان حزم جميع ملفات PDF والصور بدون ضغط
    androidResources {
        noCompress += listOf("pdf", "png", "jpg", "jpeg")
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            // لا تستثني أي ملفات
            excludes -= setOf("**/*.pdf")
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("intermediates/shared_assets"))
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(compose.material3)

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation(libs.coil.compose)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(copySharedResources)
}
