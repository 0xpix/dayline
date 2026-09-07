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
            // v0.15.0.beta: Daily Flow reliability milestone — Today interactions,
            // two-way calendar sync, recurrence, Upcoming, reminders, navigation,
            // and a larger two-line Focus timer on the Phone (4a) Pro Glyph Matrix.
            versionCode = 1500
            versionName = "0.15.0"
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1"
        )
    }

    signingConfigs {
        create("betaRelease") {
            val keystoreFile = System.getenv("DAYLINE_KEYSTORE_FILE")
            if (!keystoreFile.isNullOrBlank()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("DAYLINE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DAYLINE_KEY_ALIAS")
                keyPassword = System.getenv("DAYLINE_KEY_PASSWORD")
            }
        }
        create("playRelease") {
            val keystoreFile = System.getenv("DAYLINE_PLAY_KEYSTORE_FILE")
            if (!keystoreFile.isNullOrBlank()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("DAYLINE_PLAY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DAYLINE_PLAY_KEY_ALIAS")
                keyPassword = System.getenv("DAYLINE_PLAY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = null
        }
    }

    androidComponents {
        beforeVariants(selector().withBuildType("release")) { variantBuilder ->
            when (variantBuilder.productFlavors.firstOrNull()?.second) {
                "beta" -> variantBuilder.signingConfig = signingConfigs.getByName("betaRelease")
                "play" -> variantBuilder.signingConfig = signingConfigs.getByName("playRelease")
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    val glyphMatrixSdk = file("libs/glyph-matrix-sdk-2.0.aar")
    if (glyphMatrixSdk.exists()) {
        add("betaImplementation", files(glyphMatrixSdk))
    }
}
