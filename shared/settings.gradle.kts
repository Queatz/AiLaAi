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
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
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
