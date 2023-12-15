plugins {
    kotlin("multiplatform") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
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
                val ktorVersion = "2.3.6"
                implementation(project(":models"))
                implementation(project(":widgets"))
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
        val jvmMain by getting
        val jsMain by getting
    }
}
