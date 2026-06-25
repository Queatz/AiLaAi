group = "app.ailaai.shared"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "2.4.10-RC" apply false
    kotlin("plugin.serialization") version "2.4.10-RC" apply false
    kotlin("plugin.compose") version "2.4.10-RC" apply false
    id("com.ailaai.shared.config")
}
