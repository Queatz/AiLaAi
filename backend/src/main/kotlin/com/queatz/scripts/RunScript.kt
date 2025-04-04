package com.queatz.scripts

import ScriptRender
import ScriptWithMavenDeps
import com.queatz.db.InventoryItem
import com.queatz.db.Person
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import com.queatz.db.StoryContent
import com.queatz.db.equippedItemsOfInventory
import com.queatz.db.inventoryOfPerson
import com.queatz.plugins.db
import kotlinx.serialization.json.JsonNull.content
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

private val scriptCache = mutableMapOf<String, ResultWithDiagnostics<CompiledScript>>()

class RunScript(private val script: Script, private val data: String?) {
    suspend fun run(person: Person?): ScriptResult {
        var content: List<StoryContent>? = null

        val host = BasicJvmScriptingHost()

        val scriptSource = ensurePackageDeclaration(
            source = "@file:Suppress(\"PROVIDED_RUNTIME_TOO_LOW\")\n${script.source!!}",
            packageName = "script_${script.id!!}"
        )

        val compiledScript = scriptCache[scriptSource] ?: host.compiler.invoke(
            script = scriptSource.toScriptSource(),
            scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptWithMavenDeps> {
                providedProperties(
                    "me" to Person::class.createType(nullable = true),
                    "self" to String::class.createType(),
                    "render" to ScriptRender::class.createType(),
                    "http" to ScriptHttp::class.createType(),
                    "data" to String::class.createType(nullable = true),
                    "equipment" to typeOf<() -> List<InventoryItem>>()
                )

                // todo script deps!
//                importScripts(
//                    "".toScriptSource(),
//                    "".toScriptSource()
//                )
            }
        )

        if (compiledScript.isError()) {
            return ScriptResult(
                content = listOf(
                    StoryContent.Text(
                        """Script compile error:  
                            ```${
                            compiledScript.reports.joinToString("\n")
                            }
                            ```
                        """.trimIndent()
                    )
                )
            )
        } else {
            scriptCache[scriptSource] = compiledScript
        }

        val result = host.evaluator(
            compiledScript = compiledScript.valueOrThrow(),
            scriptEvaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ScriptWithMavenDeps> {
                providedProperties(
                    "me" to person?.let { person ->
                        Person().apply {
                            id = person.id
                            name = person.name
                            photo = person.photo
                            language = person.language
                            utcOffset = person.utcOffset
                            seen = person.seen
                        }
                    },
                    "self" to script.id!!,
                    "render" to ScriptRender { content = it },
                    "http" to ScriptHttp(),
                    "data" to this@RunScript.data,
                    "equipment" to {
                        person?.let { person ->
                            db.equippedItemsOfInventory(
                                db.inventoryOfPerson(person.id!!).id!!
                            )
                        }
                    }
                )
            }
        )

        val resultError = (result as? ResultWithDiagnostics.Success)?.value?.returnValue as? ResultValue.Error

        return if (result.isError() || resultError != null) {
            ScriptResult(
                content = listOf(
                    StoryContent.Text(
                        """Script run error:  
                            ```
                            ${resultError?.let { "$it\n\n" }}${
                            result.reports.joinToString("\n")
                            }
                            ```
                        """.trimIndent()
                    )
                )
            )
        } else {
            ScriptResult(content = content)
        }
    }
}

/**
 * Inserts a package declaration after @file: annotations if none exists
 *
 * @param source The original script source code
 * @param packageName The package name to insert (defaults to "generated")
 * @return The modified source with package declaration
 */
fun ensurePackageDeclaration(
    source: String,
    packageName: String = "script"
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
