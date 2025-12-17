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
                "8.13.1"
            }
        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        classpath("com.huawei.agconnect:agcp:1.9.1.300")
        classpath("com.google.gms:google-services:4.4.4")
        classpath("io.objectbox:objectbox-gradle-plugin:5.0.1")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
