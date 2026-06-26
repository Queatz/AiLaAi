buildscript {
    repositories {
        maven(url = "https://developer.huawei.com/repo/")
        google()
        mavenCentral()
    }
    dependencies {
        val properties = java.util.Properties().apply {
            load(file("local.properties").inputStream())
        }
        val agpVersion = properties.getProperty("AGP_VERSION")
            .orEmpty()
            .ifBlank {
                "9.2.0"
            }
        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath(libs.kotlin.gradle.plugin)
        classpath("com.huawei.agconnect:agcp:1.9.1.300")
        classpath("com.google.gms:google-services:4.5.0")
        classpath("io.objectbox:objectbox-gradle-plugin:5.4.2")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.3.8" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
