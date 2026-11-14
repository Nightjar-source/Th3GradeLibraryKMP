import java.util.Properties
import java.io.FileInputStream

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
        versionCode = 12096
        versionName = "1.2.96"
    }
    
    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    // Signing secrets are NEVER hardcoded: local builds read local.properties / env vars,
    // CI reads GitHub Secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_PASSWORD).
    signingConfigs {
        create("release") {
            storeFile = file("Nightjar.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "Nightjar"
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
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
            assets.srcDirs(layout.buildDirectory.dir("intermediates/shared_assets"))
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
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.browser)
    implementation(libs.coil.compose)
    // Official Google Mobile Ads SDK dependency ready (zero active ad calls in UI)
    implementation(libs.play.services.ads)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(copySharedResources)
}
