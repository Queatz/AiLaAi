plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
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
                val ktorVersion = "2.3.9"
                implementation(project(":models"))
                implementation(project(":widgets"))
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        val jvmMain by getting
        val jsMain by getting
    }
}
