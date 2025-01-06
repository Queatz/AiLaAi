pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform") version "2.1.0"
        id("org.jetbrains.compose") version "1.7.3"
    }
}

includeBuild("../shared")

rootProject.name = "ailaai"
