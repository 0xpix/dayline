plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pix.dayline"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pix.dayline"
        minSdk = 26
        targetSdk = 36
        // Keep the base/Play version on the current milestone. Beta can override
        // this independently so GitHub prereleases remain installable updates.
        versionCode = 1400
        versionName = "0.14.0"

        val commit = (System.getenv("GITHUB_SHA") ?: "local").take(7)
        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("beta") {
            dimension = "distribution"
            applicationIdSuffix = ".beta"
            versionNameSuffix = ".beta"
            // v0.16.1.beta: navigation + updater presentation polish.
            // Swipe navigation stays, while page swaps are intentionally instant.
            versionCode = 1601
            versionName = "0.16.1"
            // Nothing's Glyph Matrix SDK 2.0 declares minSdk 33. Keep this
            // requirement isolated to the beta/Glyph build so the normal Play
            // build continues to support Dayline's global minSdk 26.
            minSdk = 33
            buildConfigField("boolean", "GITHUB_BETA_UPDATES", "true")
            buildConfigField("String", "UPDATE_CHANNEL", "\"GitHub beta\"")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "GITHUB_BETA_UPDATES", "false")
            buildConfigField("String", "UPDATE_CHANNEL", "\"Play\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val releaseStorePath = System.getenv("DAYLINE_KEYSTORE_FILE")
    val releaseStorePassword = System.getenv("DAYLINE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("DAYLINE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("DAYLINE_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseStorePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    val glyphMatrixSdk = file("libs/glyph-matrix-sdk-2.0.aar")
    if (glyphMatrixSdk.exists()) {
        add("betaImplementation", files(glyphMatrixSdk))
    }
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}