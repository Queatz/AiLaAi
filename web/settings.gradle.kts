pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform") version "2.4.0"
            id("org.jetbrains.compose") version "1.11.0"
    }
}

includeBuild("../shared")

rootProject.name = "ailaai"
