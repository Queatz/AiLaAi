package com.queatz.scripts

import com.queatz.db.InventoryItemExtended
import com.queatz.db.Message
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SerializationPluginTest {

    /**
     * Reproduces the issue where the kotlinx.serialization compiler plugin is not applied when
     * compiling Kotlin scripts, causing `Serializer for class '...' is not found.` at runtime even
     * though the class is annotated with `@Serializable`.
     */
    @Test
    fun `serializable class in script can be serialized`() {
        val source = """
            import kotlinx.serialization.Serializable
            import kotlinx.serialization.encodeToString
            import kotlinx.serialization.json.Json

            @Serializable
            data class DeepSeekWordInfo(
                val original: String = "",
                val translation: String = "",
                val sentences: List<String> = emptyList(),
                val englishTranslations: List<String> = emptyList()
            )

            val info = DeepSeekWordInfo(
                original = "hello",
                translation = "hola",
                sentences = listOf("Hello there."),
                englishTranslations = listOf("hi")
            )

            Json.encodeToString(info)
        """.trimIndent()

        val result = runScript(source)

        val value = (result.valueOrThrow().returnValue as ResultValue.Value).value as String

        assertEquals(
            """{"original":"hello","translation":"hola","sentences":["Hello there."],"englishTranslations":["hi"]}""",
            value
        )
    }

    /**
     * Reproduces the issue for a `@Serializable` class declared in a script that is pulled in via
     * `@file:DependsOnScript`. The serialization compiler plugin must be applied to imported script
     * compilation units as well, otherwise serializing the imported class fails at runtime.
     */
    @Test
    fun `serializable class in imported script can be serialized`() {
        val loadedScripts = mutableMapOf<String, com.queatz.db.Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        loadedScripts["wordinfo"] = com.queatz.db.Script().apply {
            id = "wordinfo"
            person = "person1"
            source = """
                import kotlinx.serialization.Serializable
                import kotlinx.serialization.encodeToString
                import kotlinx.serialization.json.Json

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = "",
                    val sentences: List<String> = emptyList(),
                    val englishTranslations: List<String> = emptyList()
                )

                fun deepSeekJson(): String = Json.encodeToString(
                    DeepSeekWordInfo(
                        original = "hello",
                        translation = "hola",
                        sentences = listOf("Hello there."),
                        englishTranslations = listOf("hi")
                    )
                )
            """.trimIndent()
        }

        val source = """
            @file:DependsOnScript("wordinfo")

            deepSeekJson()
        """.trimIndent()

        val result = runScript(source)

        val value = (result.valueOrThrow().returnValue as ResultValue.Value).value as String

        assertEquals(
            """{"original":"hello","translation":"hola","sentences":["Hello there."],"englishTranslations":["hi"]}""",
            value
        )
    }

    @Test
    fun `serializable class in script can be serialized reflectively`() {
        val source = """
            import kotlinx.serialization.serializer
            import kotlinx.serialization.json.Json
            import kotlin.reflect.typeOf

            @Serializable
            data class DeepSeekWordInfo(
                val original: String = "",
                val translation: String = ""
            )

            val info = DeepSeekWordInfo(original = "hello", translation = "hola")
            Json.encodeToString(serializer(typeOf<DeepSeekWordInfo>()), info)
        """.trimIndent()

        val result = runScript(source)

        val value = (result.valueOrThrow().returnValue as ResultValue.Value).value as String

        assertEquals(
            """{"original":"hello","translation":"hola"}""",
            value
        )
    }

    private fun runScript(source: String): ResultWithDiagnostics<EvaluationResult> {
        val host = BasicJvmScriptingHost()

        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptWithMavenDeps>()

        val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ScriptWithMavenDeps> {
            providedProperties(
                "me" to null,
                "self" to "test",
                "render" to ScriptRender { },
                "app" to TestScriptApp,
                "storage" to TestScriptStorage,
                "http" to ScriptHttp(),
                "data" to null,
                "input" to null,
                "secret" to null,
                "equipment" to { null as List<InventoryItemExtended>? }
            )
        }

        return runBlocking {
            val compiledScript = host.compiler.invoke(
                script = source.toScriptSource("script_test"),
                scriptCompilationConfiguration = compilationConfiguration
            )

            assertFalse(
                compiledScript.isError(),
                "Script compilation failed: ${compiledScript.reports.joinToString("\n")}"
            )

            host.evaluator(
                compiledScript = compiledScript.valueOrThrow(),
                scriptEvaluationConfiguration = evaluationConfiguration
            )
        }
    }
}

private object TestScriptApp : ScriptApp {
    override suspend fun download(url: String, name: String): String = throw NotImplementedError()
    override suspend fun message(groupId: String, text: String): Message = throw NotImplementedError()
}

private object TestScriptStorage : ScriptStorage {
    override fun get(key: String): String? = throw NotImplementedError()
    override fun set(key: String, value: String?) = throw NotImplementedError()
    override fun <T : Any> put(collection: KClass<T>, value: T): T = throw NotImplementedError()
    override fun <T : Any> get(collection: KClass<T>, key: String): T? = throw NotImplementedError()
    override fun <T : Any> all(collection: KClass<T>): List<T> = throw NotImplementedError()
    override fun <T : Any> keys(collection: KClass<T>): List<String> = throw NotImplementedError()
    override fun <T : Any> delete(collection: KClass<T>, key: String) = throw NotImplementedError()
    override fun <T : Any> query(collection: KClass<T>, aql: String, params: Map<String, Any?>): List<T> =
        throw NotImplementedError()

    override fun <T : Any> collection(collection: KClass<T>): String = throw NotImplementedError()
    override fun <T : Any> hasCollection(collection: KClass<T>): Boolean = throw NotImplementedError()
}
