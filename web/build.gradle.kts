import org.codehaus.groovy.ast.tools.GeneralUtils.args

plugins {
    kotlin("multiplatform") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    kotlin("plugin.compose") version "2.2.20"
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

tasks.register("run") {
    group = "application"
    description = "Runs the Kotlin/JS webpack dev server"
    dependsOn("jsBrowserDevelopmentRun")
}

defaultTasks("run")

kotlin {
    js(IR) {
        browser {
            runTask {
                args("-t")
                devServerProperty.set(devServerProperty.get().copy(port = 4040))
            }
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
            }
        }
        binaries.executable()
        useEsModules()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("app.ailaai.shared:push")
                implementation("app.ailaai.shared:models")
                implementation("app.ailaai.shared:api")
                implementation("app.ailaai.shared:widgets")
                implementation("app.ailaai.shared:reminders")
                implementation("app.ailaai.shared:content")
                implementation("io.ktor:ktor-client-js:${versions.ktor}")
                implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
                implementation("io.ktor:ktor-serialization-kotlinx-json:${versions.ktor}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
                implementation("app.softwork:routing-compose:0.5.0")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2025.10.10")
                implementation(npm("qr", "0.5.2"))
                implementation(npm("date-fns", "4.1.0"))
                implementation(npm("@vvo/tzdb", "6.187.0"))
                implementation(npm("@videosdk.live/js-sdk", "0.3.8"))
                implementation(npm("mapbox-gl", "3.15.0"))
                implementation(npm("marked", "16.4.1"))
                implementation(npm("@babylonjs/core", "8.32.2"))
                implementation(npm("@babylonjs/materials", "8.32.2"))
                implementation(npm("monaco-editor", "0.54.0"))
            }
        }
    }
}
