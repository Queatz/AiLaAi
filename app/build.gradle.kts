@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("plugin.serialization") version "1.8.10"
//    id("com.huawei.agconnect") // todo IncrementalTaskInputs, try again after April, 2023
    id("com.google.gms.google-services")
}

val properties = Properties()
properties.load(file("../local.properties").inputStream())

android {
    compileSdk = 33
    namespace = "com.queatz.ailaai"

    defaultConfig {
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = properties.getProperty("GOOGLE_MAPS_API_KEY")
        manifestPlaceholders["HMS_APP_ID"] = properties.getProperty("HMS_APP_ID")

        applicationId = "com.ailaai.app"
        minSdk = 26
        targetSdk = 33
        versionCode = 29
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
//        resConfigs = "en", "vn"
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-client-core-jvm:2.3.0")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.0")
    val choiceSdkVersion = "0.3.0"
    val hmsVersion = "6.9.0.300"
    val composeVersion = "1.4.0"
    val ktorVersion = "2.2.4"

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha02")
    implementation("androidx.appcompat:appcompat-resources:1.7.0-alpha02")
    implementation("com.google.android.material:material:1.8.0") // todo is this needed?
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("androidx.compose.material3:material3:1.1.0-alpha06")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.0-alpha06")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    runtimeOnly("androidx.compose.runtime:runtime-rxjava3:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
    implementation("androidx.activity:activity-compose:1.7.0-alpha02")
    implementation("at.bluesource.choicesdk:choicesdk-location:$choiceSdkVersion")
    implementation("at.bluesource.choicesdk:choicesdk-maps:$choiceSdkVersion")
    implementation("at.bluesource.choicesdk:choicesdk-messaging:$choiceSdkVersion")
    implementation("com.huawei.hms:base:$hmsVersion")
    implementation("com.huawei.hms:maps:$hmsVersion")
    implementation("com.huawei.hms:push:$hmsVersion")
    implementation("com.huawei.hms:scan:2.9.0.300")
    implementation("com.huawei.hms:hianalytics:$hmsVersion")
    implementation("com.huawei.hms:hianalytics:$hmsVersion")
    implementation("com.huawei.hms:hwid:$hmsVersion")
    implementation("com.huawei.hms:location:$hmsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("androidx.fragment:fragment-ktx:1.5.6")
    implementation("androidx.compose.ui:ui-viewbinding:1.4.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation("com.google.accompanist:accompanist-permissions:0.27.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation(platform("com.google.firebase:firebase-bom:31.0.2"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}
