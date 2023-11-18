@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("plugin.serialization") version "1.9.20"
    id("com.huawei.agconnect")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
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
        versionCode = 69
        versionName = "0.9.69"

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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
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
    val ktorVersion = "2.3.6"
    val choiceSdkVersion = "0.3.0"
    val composeVersion = "1.5.4"
    implementation("app.ailaai.shared:models")
    implementation("app.ailaai.shared:push")
    implementation("app.ailaai.shared:api")
    implementation("app.ailaai.shared:widgets")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("androidx.appcompat:appcompat-resources:1.7.0-alpha03")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    runtimeOnly("androidx.compose.runtime:runtime-rxjava3:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("at.bluesource.choicesdk:choicesdk-location:$choiceSdkVersion")
    implementation("at.bluesource.choicesdk:choicesdk-maps:$choiceSdkVersion")
    implementation("at.bluesource.choicesdk:choicesdk-messaging:$choiceSdkVersion")
    implementation("com.huawei.hms:base:6.11.0.301")
    implementation("com.huawei.hms:maps:6.11.0.304")
    implementation("com.huawei.hms:push:6.11.0.300")
    implementation("com.huawei.hms:scan:2.11.0.300")
    implementation("com.huawei.hms:hianalytics:6.10.0.303")
    implementation("com.huawei.hms:hwid:6.11.0.300")
    implementation("com.huawei.hms:location:6.11.0.301")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.compose.ui:ui-viewbinding:$composeVersion")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("me.saket.telephoto:zoomable-image-coil:1.0.0-alpha02")
    implementation("com.google.accompanist:accompanist-permissions:0.28.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.media3:media3-exoplayer:1.1.1")
    implementation("androidx.media3:media3-ui:1.1.1")
    implementation("com.otaliastudios:transcoder:0.10.5")
    implementation("ch.acra:acra-core:5.11.3")
    implementation("ch.acra:acra-toast:5.11.3")
    implementation("com.ibm.icu:icu4j:73.2")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.1.0")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
}
