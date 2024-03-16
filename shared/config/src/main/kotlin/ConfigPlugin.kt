package com.ailaai.shared.config

import org.gradle.api.Plugin
import org.gradle.api.Project

class ConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("versions", Versions::class.java)
    }
}
