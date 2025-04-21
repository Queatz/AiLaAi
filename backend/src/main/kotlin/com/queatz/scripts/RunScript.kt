package com.queatz.scripts

import ScriptRender
import ScriptStorage
import ScriptWithMavenDeps
import ScriptWithMavenDepsConfiguration.scriptLoader
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Person
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import com.queatz.db.StoryContent
import com.queatz.db.equippedItemsOfInventory
import com.queatz.db.inventoryOfPerson
import com.queatz.db.scriptData
import com.queatz.plugins.db
import com.queatz.scripts.store.ArangoScriptStorage
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
    private val input: Map<String, String?>?,
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
                "storage" to ArangoScriptStorage(
                    scriptOwner = script.person!!,
                    person = person?.id
                ),
                "http" to ScriptHttp(),
                "data" to this@RunScript.data,
                "input" to this@RunScript.input,
                "secret" to db.scriptData(script.id!!)?.secret,
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
            println("Compilation failed for script id ${script.id}: ${compiledScript.reports.joinToString("\n")}")
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

        val result = runInSandbox {
            host.evaluator(
                compiledScript = compiledScript.valueOrThrow(),
                scriptEvaluationConfiguration = scriptEvaluationConfiguration
            )
        }

        val resultError = (result as? ResultWithDiagnostics.Success)?.value?.returnValue as? ResultValue.Error

        return if (result.isError() || resultError != null) {
            println(
                "Execution failed for script id ${script.id}: ${
                resultError?.let { "$it\n\n" } ?: "Unknown runtime error."
            } ${result.reports.joinToString("\n")}")
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

suspend fun <T> runInSandbox(block: suspend () -> T): T {
    // Configure thread context class loader with restricted access
    val originalClassLoader = Thread.currentThread().contextClassLoader
    try {
        val restrictedClassLoader = RestrictedClassLoader(originalClassLoader)
        Thread.currentThread().contextClassLoader = restrictedClassLoader
        return block()
    } finally {
        Thread.currentThread().contextClassLoader = originalClassLoader
    }
}

class RestrictedClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    private val blockedPackages = setOf(
        "java.io",
        "java.nio",
        "kotlin.io",
        "java.lang.System.getProperty",
        "java.lang.System.getenv",
        "java.lang.System.exit",
        "java.lang.System.gc",
        "java.lang.System.setIn",
        "java.lang.System.setOut",
        "java.lang.System.setErr",
        "java.util.logging",
        "java.util.jar",
        "java.util.zip",
        "org.apache.commons.io",
        "com.google.common.io"
    )

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // Block loading of file system access classes
        if (blockedPackages.any { name.startsWith(it) }) {
            throw ClassNotFoundException("Access to $name is restricted for security reasons")
        }
        return super.loadClass(name, resolve)
    }
}
