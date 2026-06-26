dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../shared")

rootProject.name = "Ai La Ai Backend"

include("scripts")
