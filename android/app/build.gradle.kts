@file:Suppress("UnstableApiUsage", "PackageDirectoryMismatch")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    id("com.android.legacy-kapt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    id("com.huawei.agconnect")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.ailaai.shared.config")
    id("io.objectbox")
}

val properties = Properties()
val localPropertiesFile = file("../local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

android {
    compileSdk = 37
    namespace = "com.queatz.ailaai"

    defaultConfig {
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = properties.getProperty("GOOGLE_MAPS_API_KEY")
        manifestPlaceholders["HMS_APP_ID"] = properties.getProperty("HMS_APP_ID")

        applicationId = "com.ailaai.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 109
        versionName = "1.0.9"

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
        create("local") {
            initWith(getByName("debug"))
            isDebuggable = true
            matchingFallbacks += listOf("debug")
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

    flavorDimensions += "variant"

    productFlavors {
        create("default") {
            dimension = "variant"
            manifestPlaceholders["IMPROMPTU_ENABLED"] = "merge" // true
            buildConfigField("boolean", "ENABLE_BACKGROUND_LOCATION", "true")
        }
        create("google") {
            dimension = "variant"
            manifestPlaceholders["IMPROMPTU_ENABLED"] = "remove" // false
            buildConfigField("boolean", "ENABLE_BACKGROUND_LOCATION", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        localeFilters.addAll(setOf("en", "vi"))
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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
    implementation("app.ailaai.shared:content")

    // Ktor
    implementation("io.ktor:ktor-client-core-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-websockets-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${versions.ktor}")

    // Compose
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.compose.ui:ui:${versions.compose}")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:${versions.compose}")
    implementation("androidx.compose.ui:ui-viewbinding:${versions.compose}")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")

    // Android
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutines}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.appcompat:appcompat-resources:1.8.0")
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")
    implementation("dev.shreyaspatil:capturable:3.0.1")
    implementation("com.github.skydoves:colorpicker-compose:1.3.0")
    implementation("io.github.ehsannarmani:compose-charts:1.0.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:${versions.markdownRenderer}")
    implementation("com.mikepenz:multiplatform-markdown-renderer-coil3:${versions.markdownRenderer}")
    implementation("com.mikepenz:multiplatform-markdown-renderer-code:${versions.markdownRenderer}")
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    // Google Maps, Location & Firebase Messaging
    implementation("com.google.android.gms:play-services-base:18.11.1")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation("com.google.firebase:firebase-messaging:26.0.2")

    // HMS Support
    implementation("com.huawei.hms:base:6.13.0.300")
    implementation("com.huawei.hms:scanplus:2.15.0.301")
    implementation("com.huawei.hms:hwid:6.14.0.300")

    // Media
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.3")
    implementation("me.saket.telephoto:zoomable-image-coil3:0.19.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("com.otaliastudios:transcoder:0.11.2")

    // Logging & Crash Reporting
    implementation("ch.acra:acra-core:5.14.2")
    implementation("ch.acra:acra-toast:5.14.2")
    implementation("com.ibm.icu:icu4j:79.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.2.0")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")

    // VideoSDK
    implementation("live.videosdk:rtc-android-sdk:0.2.0") { // todo 1.2.1
        exclude("androidx.core")
        exclude("com.android.support")
    }

    // Development
    debugImplementation("androidx.compose.ui:ui-tooling:${versions.compose}")
}
