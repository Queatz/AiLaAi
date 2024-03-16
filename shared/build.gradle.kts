group = "app.ailaai.shared"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("com.ailaai.shared.config")
}
