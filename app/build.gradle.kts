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
        // v0.12.8.beta used versionCode 33. Keep a large monotonic code so
        // Android accepts this beta and leaves room for 0.13.x follow-ups.
        versionCode = 1301
        versionName = "0.13.1"

        val commit = (System.getenv("GITHUB_SHA") ?: "local").take(7)
        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("beta") {
            dimension = "distribution"
            applicationIdSuffix = ".beta"
            versionNameSuffix = ".beta"
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
