plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.ailaai.shared.config")
}

group = "app.ailaai.shared"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    js {
        browser {

        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":models"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
            }
        }
        val jvmMain by getting
        val jsMain by getting
    }
}
