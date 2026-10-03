plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseStorePath = providers.environmentVariable("QURAN_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("QURAN_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("QURAN_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("QURAN_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(
    releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.qurankareem.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.qurankareem.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.7.0"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:quran"))
    implementation(project(":feature:mushaf"))
    implementation(project(":feature:home"))
    implementation(project(":core:audio"))
    implementation(project(":core:prayer"))
    implementation(project(":feature:audio"))
    implementation(project(":feature:prayer"))
    implementation(project(":feature:qibla"))
    implementation(project(":core:settings"))
    implementation(project(":feature:settings"))
    implementation(project(":core:practice"))
    implementation(project(":feature:hifz"))
    implementation(project(":feature:khatma"))
    implementation(project(":feature:adhkar"))
    implementation(project(":feature:stats"))
    implementation(project(":core:stats"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.kotlinx.coroutines.android)
}
