package com.queatz.scripts

import com.queatz.db.InventoryItemExtended
import com.queatz.db.Person
import com.queatz.db.Script
import com.queatz.scripts.ScriptWithMavenDepsConfiguration.scriptLoader
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.full.createType
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
import kotlin.script.experimental.api.providedProperties
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

@Repeatable
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
            SerialName::class,
            Json::class
        )

        // Declared on the script definition (not only at runtime in RunScript) so that scripts
        // pulled in via @file:DependsOnScript inherit the same provided properties and can
        // reference them (e.g. `secret`, `render`). Values are supplied by RunScript at evaluation.
        providedProperties(
            "me" to Person::class.createType(nullable = true),
            "self" to String::class.createType(),
            "render" to ScriptRender::class.createType(),
            "app" to ScriptApp::class.createType(),
            "storage" to ScriptStorage::class.createType(),
            "http" to ScriptHttp::class.createType(),
            "data" to String::class.createType(nullable = true),
            "input" to Map::class.createType(
                arguments = listOf(
                    invariant(String::class.createType()),
                    invariant(String::class.createType(nullable = true))
                ),
                nullable = true,
            ),
            "secret" to String::class.createType(nullable = true),
            "equipment" to Function0::class.createType(
                arguments = listOf(
                    invariant(
                        List::class.createType(
                            arguments = listOf(
                                invariant(InventoryItemExtended::class.createType())
                            ),
                            nullable = true
                        )
                    )
                )
            )
        )

        jvm {
            jvmTarget("21")
            dependenciesFromCurrentContext(
                wholeClasspath = true,
                unpackJarCollections = true
            )
            compilerOptions(
                "-Xadd-modules=ALL-MODULE-PATH"
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

/**
 * Resolves the scripts referenced via @file:DependsOnScript and makes their declarations available
 * to the importing script through the compiler's `importScripts` mechanism (rather than inlining
 * the sources). Dependencies are resolved transitively and recursively, so a deep / branching
 * (diamond) dependency tree is fully imported; the compiler de-duplicates repeated imports.
 *
 * Keeping each script as its own compilation unit (instead of merging all sources into one) is the
 * proper, scalable approach for large dependency trees.
 *
 * Known limitation (KT-86352, https://youtrack.jetbrains.com/issue/KT-86352): under K2 in Kotlin
 * 2.4.0, referencing a type declared in an imported script from an *explicit type position* in the
 * importing script (e.g. `val d: List<WordPracticeData> = ...`) crashes FIR analysis with
 * "Expected FirResolvedTypeRef with ConeKotlinType but was FirUserTypeRefImpl". Using the imported
 * type via inference (`val d = ...`) or only calling imported functions works fine. The compiler
 * fix is planned for Kotlin 2.4.10; once that version is adopted no change is needed here.
 */
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

    // The current script's source is named "script_<id>" (see RunScript and below). Skip importing
    // a script into itself, which would otherwise be reported as a recursive dependency cycle.
    val currentScriptId = context.script.name
        ?.removeSuffix(".kts")
        ?.removePrefix("script_")

    val scripts = scriptIds
        .filterNot { it == currentScriptId }
        .mapNotNull { scriptId ->
            parseScript(
                scriptLoader(scriptId) ?: return@mapNotNull null
            ).toScriptSource("script_$scriptId.kts")
        }

    return context.compilationConfiguration.with {
        importScripts.append(scripts)
    }.asSuccess()
}

/**
 * Returns the script source to compile.
 *
 * Note: a per-script `package` declaration must NOT be added here. Scripts referenced via
 * @file:DependsOnScript are compiled as imported sources in the same module, and the compiler only
 * makes their declarations (and applies default imports / provided properties) visible to the
 * importing script when they share the (default) package.
 */
fun parseScript(script: Script): String = script.source!!
