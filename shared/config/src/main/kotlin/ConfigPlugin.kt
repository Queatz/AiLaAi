package com.ailaai.shared.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

class ConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val catalog = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        project.extensions.create("versions", Versions::class.java, catalog)
    }
}
