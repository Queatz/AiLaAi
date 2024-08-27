plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.ailaai.shared.config")
}

group = "app.ailaai.shared"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
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
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-core:${versions.jackson}")
                implementation("com.fasterxml.jackson.core:jackson-databind:${versions.jackson}")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${versions.jackson}")
                implementation("com.arangodb:jackson-serde-json:${versions.arango}")
            }
        }
        val jsMain by getting
    }
}
