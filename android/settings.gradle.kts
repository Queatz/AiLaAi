dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://developer.huawei.com/repo/")
        maven(url = "https://jitpack.io")
        google()
        mavenCentral()
        maven(url = "https://maven.aliyun.com/repository/jcenter")
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include(":app")

includeBuild("../shared")

rootProject.name = "Ai La Ai"
