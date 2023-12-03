pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(
    "models",
    "push",
    "api",
    "widgets"
)

rootProject.name = "ailaai-shared"
