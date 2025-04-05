package com.queatz.scripts

import ScriptRender
import ScriptWithMavenDeps
import ScriptWithMavenDepsConfiguration.scriptLoader
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Person
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import com.queatz.db.StoryContent
import com.queatz.db.equippedItemsOfInventory
import com.queatz.db.inventoryOfPerson
import com.queatz.plugins.db
import parseScript
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.full.createType
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

// Todo FIFO max 1000
// Todo bust cache if ANY dependent scripts change
private val scriptCache = mutableMapOf<String, ResultWithDiagnostics<CompiledScript>>()

class RunScript(
    private val script: Script,
    private val data: String?,
    private val useCache: Boolean = true,
) {
    init {
        scriptLoader = { scriptId ->
            db.document(Script::class, scriptId)
        }
    }

    suspend fun run(person: Person?): ScriptResult {
        var content: List<StoryContent>? = null

        val host = BasicJvmScriptingHost()

        val scriptSource = parseScript(script)

        val scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptWithMavenDeps> {
            providedProperties(
                "me" to Person::class.createType(nullable = true),
                "self" to String::class.createType(),
                "render" to ScriptRender::class.createType(),
                "http" to ScriptHttp::class.createType(),
                "data" to String::class.createType(nullable = true),
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
        }

        val scriptEvaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ScriptWithMavenDeps> {
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

        val compiledScript = scriptCache[scriptSource].takeIf { useCache } ?: host.compiler.invoke(
            script = scriptSource.toScriptSource("script_${script.id!!}"),
            scriptCompilationConfiguration = scriptCompilationConfiguration
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
            scriptEvaluationConfiguration = scriptEvaluationConfiguration
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
