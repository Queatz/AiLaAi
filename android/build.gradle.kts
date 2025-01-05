buildscript {
    repositories {
        maven(url = "https://developer.huawei.com/repo/")
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
        classpath("com.huawei.agconnect:agcp:1.9.1.300")
        classpath("com.google.gms:google-services:4.4.2")
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.3")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
