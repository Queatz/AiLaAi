
val ktorVersion = "2.3.6"

plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
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
                implementation(compose.runtime)
                implementation(compose.html.core)
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("dev.opensavvy.compose.lazy:lazy-layouts-js:0.1.2") // todo: needs update
                implementation("app.softwork:routing-compose:0.2.12")
                implementation(npm("@paulmillr/qr", "0.1.1"))
                implementation(npm("date-fns", "2.30.0"))
                implementation(npm("date-fns-tz", "2.0.0"))
                implementation(npm("@vvo/tzdb", "6.110.0"))
            }
        }
    }
}
