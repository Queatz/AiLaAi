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
                implementation(project(":widgets"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
                implementation("com.arangodb:jackson-serde-json:7.5.1")
            }
        }
        val jsMain by getting
    }
}
