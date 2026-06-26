package com.ailaai.shared.config

import org.gradle.api.artifacts.VersionCatalog

open class Versions(
    private val catalog: VersionCatalog,
) {
    private fun version(
        name: String,
    ) = catalog.findVersion(name).orElseThrow {
        IllegalStateException("Missing version '$name' in the 'libs' version catalog")
    }.requiredVersion

    val kotlin = version("kotlin")
    val coroutines = version("coroutines")
    val ktor = version("ktor")
    val choiceSdk = version("choiceSdk")
    val compose = version("compose")
    val serialization = version("serialization")
    val datetime = version("datetime")
    val logback = version("logback")
    val arango = version("arango")
    val jackson = version("jackson")
    val markdownRenderer = version("markdownRenderer")
}
