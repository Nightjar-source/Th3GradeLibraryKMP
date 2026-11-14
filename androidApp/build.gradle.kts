import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
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
        applicationId = "com.Nightjar.Th3GradeLibraryKMP.cloud"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 15133
        versionName = "2.3.0-cloud"
    }
    
    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            storeFile = file("Nightjar.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: "19941994"
            keyAlias = "Nightjar"
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: "19941994"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
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
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation(libs.coil.compose)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.kotlinx.serialization.json)
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(copySharedResources)
}
