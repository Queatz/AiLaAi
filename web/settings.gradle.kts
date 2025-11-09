pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform") version "2.2.21"
            id("org.jetbrains.compose") version "1.8.2"
    }
}

includeBuild("../shared")

rootProject.name = "ailaai"
