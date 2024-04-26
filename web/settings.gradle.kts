pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform") version "1.9.22"
        id("org.jetbrains.compose") version "1.6.2"
    }
}

includeBuild("../shared")

rootProject.name = "ailaai"
