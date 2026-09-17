import org.gradle.api.tasks.Delete

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
                "9.1.0"
            }
        classpath("com.android.tools.build:gradle:$agpVersion")
        // AGP 9 ships kapt as a separate plugin; ObjectBox still generates via kapt.
        classpath("com.android.legacy-kapt:com.android.legacy-kapt.gradle.plugin:$agpVersion")
        classpath(libs.kotlin.gradle.plugin)
        classpath("com.huawei.agconnect:agcp:1.9.6.300")
        classpath("com.google.gms:google-services:4.5.0")
        classpath("io.objectbox:objectbox-gradle-plugin:6.0.0-beta")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.3.8" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
