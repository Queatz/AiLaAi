plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("config") {
            id = "com.ailaai.shared.config"
            implementationClass = "com.ailaai.shared.config.ConfigPlugin"
        }
    }
}
