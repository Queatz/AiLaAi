buildscript {
    repositories {
        maven(url = "https://developer.huawei.com/repo/")
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
        classpath("com.huawei.agconnect:agcp:1.9.1.300")
        classpath("com.google.gms:google-services:4.4.0")
    }
}

plugins {
    id("com.google.devtools.ksp") version  "1.9.21-1.0.15" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
