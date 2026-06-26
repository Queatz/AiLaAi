import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.kotlin

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
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

// The ktor fat jar (shadowJar / buildFatJar) must MERGE META-INF/services files so that the
// kotlinx.serialization compiler plugin's registration files (org.jetbrains.kotlin.compiler.plugin.
// CommandLineProcessor and CompilerPluginRegistrar) are preserved. Several compiler plugins on the
// classpath (e.g. the scripting compiler and the serialization plugin) ship service files with the
// same names; the default shadow behaviour keeps only one, dropping the serialization plugin's
// registration. The scripting compiler then can no longer discover the serialization plugin when
// compiling user scripts at runtime, so @Serializable classes in scripts get no generated serializer
// and fail with "Serializer for class '...' is not found." (this only surfaces in the packaged fat
// jar, not in installDist or tests, where the plugin jar is a separate classpath entry).
afterEvaluate {
    tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        // mergeServiceFiles only takes effect when duplicates are INCLUDEd; the EXCLUDE strategy
        // (set by the ktor plugin) drops the extra service files before they reach the transformer.
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        // The serialization compiler plugin reads the kotlinx-serialization-core runtime version from
        // the MANIFEST.MF (Implementation-Title/Version) of the jar that provides the core classes.
        // A fat jar collapses every dependency into one jar with a single manifest, so that
        // per-library version is lost and the plugin reports "kotlinx.serialization core version is
        // unknown". Re-declare it on the fat jar manifest so the plugin can detect it.
        manifest {
            attributes(
                "Implementation-Title" to "kotlinx-serialization-core",
                "Implementation-Version" to versions.serialization
            )
        }
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions.serialization}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:${versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:${versions.kotlin}")
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
