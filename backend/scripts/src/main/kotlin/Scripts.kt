import ScriptWithMavenDepsConfiguration.scriptLoader
import com.queatz.db.Script
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.with
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromScriptSourceAnnotations
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget

@KotlinScript(
    compilationConfiguration = ScriptWithMavenDepsConfiguration::class,
    fileExtension = "kts"
)
abstract class ScriptWithMavenDeps

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FILE)
annotation class DependsOnScript(val id: String)

object ScriptWithMavenDepsConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(
            DependsOn::class,
            DependsOnScript::class,
            Repository::class,
            Serializable::class,
            Json::class
        )

        jvm {
            jvmTarget("21")
            dependenciesFromCurrentContext(
                "scripts", // :scripts (this library)
                "models", // :models
                "kotlin-scripting-dependencies", // DependsOn and Repository annotation
                wholeClasspath = true,
                unpackJarCollections = true
            )
            compilerOptions(
                "-Xadd-modules=ALL-MODULE-PATH",
                "-Xplugin=kotlin-serialization-compiler-plugin-embeddable-2.1.20.jar"
            )
        }

        refineConfiguration {
            onAnnotations(
                DependsOn::class,
                Repository::class,
                handler = ::configureMavenDepsOnAnnotations
            )
            onAnnotations(
                DependsOnScript::class,
                handler = ::configureScriptDepsOnAnnotations
            )
        }
    }
) {
    lateinit var scriptLoader: (id: String) -> Script?
    private fun readResolve(): Any = ScriptWithMavenDepsConfiguration
}

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

private fun configureMavenDepsOnAnnotations(
    context: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData
        ?.get(ScriptCollectedData.collectedAnnotations)
        ?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}

private fun configureScriptDepsOnAnnotations(
    context: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val scriptIds = context.collectedData
        ?.get(ScriptCollectedData.collectedAnnotations)
        ?.takeIf { it.isNotEmpty() }
        ?.map { it.annotation }
        ?.filterIsInstance<DependsOnScript>()
        ?.map { it.id }
        ?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()

    val scripts = scriptIds.mapNotNull { scriptId ->
        parseScript(
            scriptLoader(scriptId) ?: return@mapNotNull null
        ).toScriptSource("script_$scriptId")
    }

    return context.compilationConfiguration.with {
        importScripts.append(scripts)
    }.asSuccess()
}

fun parseScript(script: Script): String = ensurePackageDeclaration(
    source = "@file:Suppress(\"PROVIDED_RUNTIME_TOO_LOW\")\n${script.source!!}",
    packageName = "script_${script.id!!}"
)

/**
 * Inserts a package declaration after @file: annotations if none exists
 *
 * @param source The original script source code
 * @param packageName The package name to insert (defaults to "generated")
 * @return The modified source with package declaration
 */
fun ensurePackageDeclaration(
    source: String,
    packageName: String,
): String {
    val lines = source.lines()

    // Check if package already exists
    if (lines.any { it.trimStart().startsWith("package ") }) {
        return source
    }

    // Find the last @file: line
    val lastFileAnnotation = lines.indexOfLast { it.trimStart().startsWith("@file:") }

    return buildString {
        // Write all lines up to insertion point
        lines.take(lastFileAnnotation + 1).forEach { appendLine(it) }

        // Insert package declaration
        appendLine("package $packageName")

        // Add blank line if needed
        if (lastFileAnnotation >= 0 && lines[lastFileAnnotation].isNotBlank()) {
            appendLine()
        }

        // Write remaining lines
        lines.drop(lastFileAnnotation + 1).forEach { appendLine(it) }
    }
}
