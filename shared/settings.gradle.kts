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
    "widgets",
    "reminders"
)

rootProject.name = "ailaai-shared"
