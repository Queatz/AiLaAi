plugins {
    kotlin("multiplatform") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    kotlin("plugin.compose") version "2.1.20"
    id("com.ailaai.shared.config")
}

group = "app.ailaai"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

kotlin {
    js(IR) {
        browser {
            runTask {
                devServer = devServer.copy(port = 4040)
            }
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("app.ailaai.shared:push")
                implementation("app.ailaai.shared:models")
                implementation("app.ailaai.shared:api")
                implementation("app.ailaai.shared:widgets")
                implementation("app.ailaai.shared:reminders")
                implementation("io.ktor:ktor-client-js:${versions.ktor}")
                implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
                implementation("io.ktor:ktor-serialization-kotlinx-json:${versions.ktor}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
                implementation("app.softwork:routing-compose:0.3.0")
                implementation(npm("@paulmillr/qr", "0.2.0"))
                implementation(npm("date-fns", "3.5.0"))
                implementation(npm("@vvo/tzdb", "6.141.0"))
                implementation(npm("@videosdk.live/js-sdk", "0.0.98"))
                implementation(npm("mapbox-gl", "3.5.2"))
                implementation(npm("marked", "15.0.7"))
                implementation(npm("monaco-editor", "0.52.2"))
            }
        }
    }
}
