dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://developer.huawei.com/repo/")
        maven(url = "https://jitpack.io")
        google()
        mavenCentral()
        jcenter()
    }
}

include(":app")

includeBuild("../shared")

rootProject.name = "Ai La Ai"
