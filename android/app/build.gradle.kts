@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("plugin.serialization") version "2.0.21"
    kotlin("plugin.compose") version "2.0.21"
    id("com.huawei.agconnect")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.ailaai.shared.config")
}

val properties = Properties()
properties.load(file("../local.properties").inputStream())

android {
    compileSdk = 34
    namespace = "com.queatz.ailaai"

    defaultConfig {
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = properties.getProperty("GOOGLE_MAPS_API_KEY")
        manifestPlaceholders["HMS_APP_ID"] = properties.getProperty("HMS_APP_ID")

        applicationId = "com.ailaai.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 95
        versionName = "0.9.95"

        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations.addAll(setOf("en", "vi"))
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    signingConfigs {
        create("release") {
            storeFile = file(properties.getProperty("storeFile"))
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")

            // This is here because of just how slow Jetpack Compose is in debug mode
            isDebuggable = false
        }
        release {
            signingConfig = signingConfigs.getByName("release")
//            isMinifyEnabled = true
//            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Ai là ai
    implementation("app.ailaai.shared:models")
    implementation("app.ailaai.shared:push")
    implementation("app.ailaai.shared:api")
    implementation("app.ailaai.shared:widgets")
    implementation("app.ailaai.shared:reminders")

    // Ktor
    implementation("io.ktor:ktor-client-core-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${versions.ktor}")

    // Compose
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.ui:ui:${versions.compose}")
    implementation("androidx.compose.material:material-icons-extended:${versions.compose}")
    implementation("androidx.compose.ui:ui-tooling-preview:${versions.compose}")
    runtimeOnly("androidx.compose.runtime:runtime-rxjava3:${versions.compose}")
    implementation("androidx.compose.ui:ui-viewbinding:${versions.compose}")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.0")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // Android
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutines}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.appcompat:appcompat-resources:1.7.0")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    implementation("dev.shreyaspatil:capturable:2.1.0")
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("io.github.ehsannarmani:compose-charts:0.0.14")

    // HMS Support
    implementation("at.bluesource.choicesdk:choicesdk-location:${versions.choiceSdk}")
    implementation("at.bluesource.choicesdk:choicesdk-maps:${versions.choiceSdk}")
    implementation("at.bluesource.choicesdk:choicesdk-messaging:${versions.choiceSdk}")
    implementation("com.huawei.hms:base:6.13.0.300")
    implementation("com.huawei.hms:maps:6.11.2.301")
    implementation("com.huawei.hms:push:6.11.0.300")
    implementation("com.huawei.hms:scan:2.12.0.301")
    implementation("com.huawei.hms:hwid:6.12.0.300")
    implementation("com.huawei.hms:location:6.12.0.300")

    // Media
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("me.saket.telephoto:zoomable-image-coil:1.0.0-alpha02")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("com.otaliastudios:transcoder:0.10.5")

    // Logging & Crash Reporting
    implementation("ch.acra:acra-core:5.11.3")
    implementation("ch.acra:acra-toast:5.11.3")
    implementation("com.ibm.icu:icu4j:74.2")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.1.0")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")

    // VideoSDK
    implementation("live.videosdk:rtc-android-sdk:0.1.36") {
        exclude("androidx.core")
        exclude("com.android.support")
    }

    // Development
    debugImplementation("androidx.compose.ui:ui-tooling:${versions.compose}")
}
