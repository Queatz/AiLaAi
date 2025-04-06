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

includeBuild("config")

include(
    "models",
    "push",
    "api",
    "widgets",
    "reminders",
    "content",
)

rootProject.name = "ailaai-shared"
