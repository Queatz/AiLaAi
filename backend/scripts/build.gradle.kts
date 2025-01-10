plugins {
    kotlin("jvm")
    id("com.ailaai.shared.config")
}

group = "com.queatz"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("app.ailaai.shared:models")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.18")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.18")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:${versions.kotlin}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${versions.datetime}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions.serialization}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
    runtimeOnly("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:${versions.kotlin}")
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
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${versions.kotlin}")
}
