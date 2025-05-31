pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform") version "2.1.20"
        id("org.jetbrains.compose") version "1.8.1"
    }
}

includeBuild("../shared")

rootProject.name = "ailaai"
