plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.2"
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
                devServer = devServer?.copy(port = 4040)
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
                implementation(compose.runtime)
                implementation(compose.html.core)
                implementation("io.ktor:ktor-client-js:${versions.ktor}")
                implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
                implementation("io.ktor:ktor-serialization-kotlinx-json:${versions.ktor}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
                implementation("dev.opensavvy.compose.lazy:lazy-layouts-js:0.2.6")
                implementation("app.softwork:routing-compose:0.2.14")
                implementation(npm("@paulmillr/qr", "0.1.1"))
                implementation(npm("date-fns", "3.5.0"))
                implementation(npm("@vvo/tzdb", "6.125.0"))
                implementation(npm("@videosdk.live/js-sdk", "0.0.80"))
            }
        }
    }
}
