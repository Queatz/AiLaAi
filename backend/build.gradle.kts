plugins {
    application
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("io.ktor.plugin") version "3.3.1"
    id("com.ailaai.shared.config")
}

group = "com.queatz"
version = "0.0.1"

application {
    mainClass.set("com.queatz.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val osName = System.getProperty("os.name").lowercase()
val tcnative_classifier = when {
    osName.contains("win") -> "windows-x86_64"
    osName.contains("linux") -> ""
    osName.contains("mac") -> "osx-x86_64"
    else -> null
}

dependencies {
    api(project(":scripts"))
    implementation("app.ailaai.shared:push")
    implementation("app.ailaai.shared:models")
    implementation("app.ailaai.shared:widgets")
    implementation("app.ailaai.shared:reminders")
    implementation("app.ailaai.shared:content")

    if (tcnative_classifier != null) {
        implementation("io.netty:netty-tcnative-boringssl-static:2.0.61.Final:linux-x86_64")
    } else {
        implementation("io.netty:netty-tcnative-boringssl-static:2.0.61.Final")
    }
    implementation("ch.qos.logback:logback-classic:${versions.logback}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:${versions.kotlin}")
    implementation("com.fasterxml.jackson.core:jackson-core:${versions.jackson}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${versions.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${versions.jackson}")
    implementation("com.arangodb:arangodb-java-driver:${versions.arango}")
    implementation("io.ktor:ktor-server-core-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-compression-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-cors-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-default-headers-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-call-logging:${versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-auth-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-host-common-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-server-netty-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${versions.ktor}")
    implementation("io.ktor:ktor-client-core-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-cio-jvm:${versions.ktor}")
    implementation("io.ktor:ktor-client-java:${versions.ktor}")
    implementation("io.ktor:ktor-server-caching-headers-jvm:${versions.ktor}")
    implementation("com.mohamedrejeb.ksoup:ksoup-html:0.6.0")
    implementation("org.apache.commons:commons-text:1.14.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${versions.kotlin}")
    testImplementation("io.ktor:ktor-server-test-host:${versions.ktor}")
}
