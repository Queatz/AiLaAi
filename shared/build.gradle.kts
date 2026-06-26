group = "app.ailaai.shared"
version = "1.0-SNAPSHOT"

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.ailaai.shared.config")
}
