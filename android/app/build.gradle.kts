@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("plugin.serialization") version "2.1.20"
    kotlin("plugin.compose") version "2.1.20"
    id("com.huawei.agconnect")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.ailaai.shared.config")
    id("io.objectbox")
}

val properties = Properties()
properties.load(file("../local.properties").inputStream())

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

android {
    compileSdk = 35
    namespace = "com.queatz.ailaai"

    defaultConfig {
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = properties.getProperty("GOOGLE_MAPS_API_KEY")
        manifestPlaceholders["HMS_APP_ID"] = properties.getProperty("HMS_APP_ID")

        applicationId = "com.ailaai.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 104
        versionName = "1.0.4"

        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
    signingConfigs {
        properties.getProperty("storeFile").let {
            if (it.isNotBlank()) {
                create("release") {
                    storeFile = file(it)
                    storePassword = properties.getProperty("storePassword")
                    keyAlias = properties.getProperty("keyAlias")
                    keyPassword = properties.getProperty("keyPassword")
                }
            }
        }
    }
    buildTypes {
        debug {
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }

            // This is here because of just how slow Jetpack Compose is in debug mode
            isDebuggable = false
        }
        release {
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        localeFilters.addAll(setOf("en", "vi"))
    }
}

dependencies {
    // Ai là ai
    implementation("app.ailaai.shared:models")
    implementation("app.ailaai.shared:push")
    implementation("app.ailaai.shared:api")
    implementation("app.ailaai.shared:widgets")
    implementation("app.ailaai.shared:reminders")
    implementation("app.ailaai.shared:content")

    // Ktor
    implementation("io.ktor:ktor-client-core-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${versions.ktor}")

    // Compose
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.compose.ui:ui:${versions.compose}")
    implementation("androidx.compose.material:material-icons-extended:${versions.compose}")
    implementation("androidx.compose.ui:ui-tooling-preview:${versions.compose}")
    runtimeOnly("androidx.compose.runtime:runtime-rxjava3:${versions.compose}")
    implementation("androidx.compose.ui:ui-viewbinding:${versions.compose}")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // Android
    implementation("com.google.accompanist:accompanist-permissions:0.37.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutines}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.appcompat:appcompat-resources:1.7.0")
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")
    implementation("dev.shreyaspatil:capturable:3.0.1")
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation("io.github.ehsannarmani:compose-charts:0.1.0")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:${versions.richtext}")

    // HMS Support
    implementation("at.bluesource.choicesdk:choicesdk-location:${versions.choiceSdk}")
    implementation("at.bluesource.choicesdk:choicesdk-maps:${versions.choiceSdk}")
    implementation("at.bluesource.choicesdk:choicesdk-messaging:${versions.choiceSdk}")
    implementation("com.huawei.hms:base:6.13.0.300")
    implementation("com.huawei.hms:maps:6.11.2.301")
    implementation("com.huawei.hms:push:6.11.0.300")
    implementation("com.huawei.hms:scanplus:2.12.0.301")
    implementation("com.huawei.hms:hwid:6.12.0.300")
    implementation("com.huawei.hms:location:6.12.0.300")

    // Media
    implementation("androidx.exifinterface:exifinterface:1.4.0")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
    implementation("me.saket.telephoto:zoomable-image-coil3:0.14.0")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("com.otaliastudios:transcoder:0.11.2")

    // Logging & Crash Reporting
    implementation("ch.acra:acra-core:5.11.3")
    implementation("ch.acra:acra-toast:5.11.3")
    implementation("com.ibm.icu:icu4j:76.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.1.0")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")

    // VideoSDK
    implementation("live.videosdk:rtc-android-sdk:0.2.0") {
        exclude("androidx.core")
        exclude("com.android.support")
    }

    // Development
    debugImplementation("androidx.compose.ui:ui-tooling:${versions.compose}")
}
