group = "app.ailaai.shared"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
    kotlin("plugin.compose") version "2.0.20" apply false
    id("com.ailaai.shared.config")
}
