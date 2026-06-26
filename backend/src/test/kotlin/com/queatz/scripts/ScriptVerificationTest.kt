package com.queatz.scripts

import com.queatz.db.Script
import com.queatz.db.StoryContent
import com.queatz.scripts.RunScript
import com.queatz.scripts.ScriptWithMavenDepsConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ScriptVerificationTest {
    @Test
    fun testSelfDependency() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "123"
            person = "person1"
            source = """
                @file:DependsOnScript("123")
                render {
                    text("Hello from Self")
                }
            """.trimIndent()
        }
        loadedScripts["123"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] SelfDependency Result: $output")
        
        assertTrue(output.contains("Hello from Self"), "Output should contain 'Hello from Self', but was: $output")
    }

    @Test
    fun testSelfDependencyWithClass() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "123"
            person = "person1"
            source = """
                @file:DependsOnScript("123")
                class MyClass {
                    val message = "Hello from Class"
                }
                render {
                    text(MyClass().message)
                }
            """.trimIndent()
        }
        loadedScripts["123"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] SelfDependencyWithClass Result: $output")

        assertTrue(output.contains("Hello from Class"), "Output should contain 'Hello from Class', but was: $output")
    }


    @Test
    fun testCrossScriptDependency() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "scriptB"
            person = "person1"
            source = """
                class CommonType {
                    val value = "CrossScriptMarker"
                }
                fun getCommon() = CommonType()
            """.trimIndent()
        }
        loadedScripts["scriptB"] = scriptB

        val scriptA = Script().apply {
            id = "scriptA"
            person = "person1"
            source = """
                @file:DependsOnScript("scriptB")
                render {
                    val common = getCommon()
                    text(common.value)
                }
            """.trimIndent()
        }
        loadedScripts["scriptA"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] CrossScriptDependency Result: $output")
        
        assertTrue(output.contains("CrossScriptMarker"), "Output should contain 'CrossScriptMarker', but was: $output")
    }

    @Test
    fun testSerialization() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "serial1"
            person = "person1"
            source = """
                import com.queatz.db.Axis

                render {
                    text(Json.encodeToString(Axis.serializer(), Axis.X))
                }
            """.trimIndent()
        }
        loadedScripts["serial1"] = scriptA

        val runner = RunScript(scriptA, null, null)
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] Serialization Result: $output")

        assertTrue(
            output.contains("\"X\""),
            "Output should contain the serialized value, but was: $output"
        )
    }

    @Test
    fun testSerializationOfScriptDefinedClass() = runBlocking {
        // Reproduces the reported issue: a @Serializable class declared *inside* the script must get
        // its serializer generated by the serialization compiler plugin. Unlike testSerialization
        // (which serializes the pre-compiled model class Axis), this exercises the plugin being
        // applied to the script's own compilation unit.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "serialDefined1"
            person = "person1"
            source = """
                import kotlinx.serialization.encodeToString

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = "",
                    val sentences: List<String> = emptyList(),
                    val englishTranslations: List<String> = emptyList()
                )

                render {
                    text(
                        Json.encodeToString(
                            DeepSeekWordInfo(original = "hello", translation = "hola")
                        )
                    )
                }
            """.trimIndent()
        }
        loadedScripts["serialDefined1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] SerializationOfScriptDefinedClass Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the serialized script-defined class, but was: $output"
        )
    }

    @Test
    fun testReflectiveSerializationOfScriptDefinedClass() = runBlocking {
        // Reflective (KType-based) serialization of a script-defined @Serializable class, executed
        // through the real RunScript sandbox (RestrictedClassLoader). This is the path that throws
        // "Serializer for class '...' is not found." at runtime when the serialization compiler
        // plugin is not applied.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "serialReflective1"
            person = "person1"
            source = """
                import kotlinx.serialization.serializer
                import kotlin.reflect.typeOf

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = ""
                )

                render {
                    val info = DeepSeekWordInfo(original = "hello", translation = "hola")
                    text(Json.encodeToString(serializer(typeOf<DeepSeekWordInfo>()), info))
                }
            """.trimIndent()
        }
        loadedScripts["serialReflective1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] ReflectiveSerializationOfScriptDefinedClass Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the reflectively serialized script-defined class, but was: $output"
        )
    }

    @Test
    fun testSerializationOfImportedScriptDefinedClass() = runBlocking {
        // The @Serializable class is declared in a script imported via @file:DependsOnScript, so the
        // serialization compiler plugin must also be applied to imported script compilation units.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "serialDep1"
            person = "person1"
            source = """
                import kotlinx.serialization.encodeToString

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = ""
                )

                fun deepSeekJson(): String = Json.encodeToString(
                    DeepSeekWordInfo(original = "hello", translation = "hola")
                )
            """.trimIndent()
        }
        loadedScripts["serialDep1"] = scriptB

        val scriptA = Script().apply {
            id = "serialMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("serialDep1")
                render {
                    text(deepSeekJson())
                }
            """.trimIndent()
        }
        loadedScripts["serialMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] SerializationOfImportedScriptDefinedClass Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the serialized imported script-defined class, but was: $output"
        )
    }

    @Test
    fun testImportedScriptUsesConfig() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        // Leaf dependency imported transitively by scriptB
        val scriptC = Script().apply {
            id = "leafC1"
            person = "person1"
            source = """
                fun helperC() = "C-helper"
            """.trimIndent()
        }
        loadedScripts["leafC1"] = scriptC

        // Imported script that itself uses DependsOnScript, Json and the `secret` provided property
        val scriptB = Script().apply {
            id = "depB1"
            person = "person1"
            source = """
                @file:DependsOnScript("leafC1")
                import com.queatz.db.Axis
                fun renderB(): String {
                    val s: String? = secret
                    return Json.encodeToString(Axis.serializer(), Axis.X) + helperC() + (s ?: "no-secret")
                }
            """.trimIndent()
        }
        loadedScripts["depB1"] = scriptB

        val scriptA = Script().apply {
            id = "mainA1"
            person = "person1"
            source = """
                @file:DependsOnScript("depB1")
                render {
                    text(renderB())
                }
            """.trimIndent()
        }
        loadedScripts["mainA1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        // RunScript.init() overrides scriptLoader with the db-backed loader; restore the mock.
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] ImportedScriptUsesConfig Result: $output")

        assertTrue(output.contains("\"X\""), "Output should contain serialized value, but was: $output")
        assertTrue(output.contains("C-helper"), "Output should contain transitive helper, but was: $output")
    }

    @Test
    fun testImportedScriptTypeInExplicitType() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "firDep1"
            person = "person1"
            source = """
                @Serializable
                data class WordPracticeData(
                    val word: String,
                    val translation: String,
                    val sentences: List<String>
                )

                fun makeData(): List<WordPracticeData> = listOf(
                    WordPracticeData("FirMarker", "b", listOf("c"))
                )

                fun renderData(items: List<WordPracticeData>): String =
                    items.joinToString { it.word }
            """.trimIndent()
        }
        loadedScripts["firDep1"] = scriptB

        val scriptA = Script().apply {
            id = "firMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("firDep1")
                render {
                    val d: List<WordPracticeData> = makeData()
                    text(renderData(d))
                }
            """.trimIndent()
        }
        loadedScripts["firMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] ImportedScriptTypeInExplicitType Result: $output")

        assertTrue(output.contains("FirMarker"), "Output should contain 'FirMarker', but was: $output")
    }

    @Test
    fun testFirCrashSelfImport() = runBlocking {
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "1585825704"
            person = "person1"
            source = """
                @file:DependsOnScript("1585825704")
                class StructuredDeepSeek {
                    fun hello() = "Hello from StructuredDeepSeek"
                }
                
                fun runTest() {
                    val s: StructuredDeepSeek = StructuredDeepSeek()
                    render {
                        text(s.hello())
                    }
                }
                
                runTest()
            """.trimIndent()
        }
        loadedScripts["1585825704"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] FirCrashSelfImport Result: $output")

        assertTrue(output.contains("Hello from StructuredDeepSeek"), "Output should contain expected text, but was: $output")
    }

    @Test
    fun testImportedTypeViaInference() = runBlocking {
        // The supported counterpart of testImportedScriptTypeInExplicitType: using a type declared
        // in an imported script via inference (no explicit type annotation) works under K2 today.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "infDep1"
            person = "person1"
            source = """
                data class Thing(val value: String)

                fun makeThing() = Thing("InferMarker")
            """.trimIndent()
        }
        loadedScripts["infDep1"] = scriptB

        val scriptA = Script().apply {
            id = "infMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("infDep1")
                render {
                    val t = makeThing()
                    text(t.value)
                }
            """.trimIndent()
        }
        loadedScripts["infMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] ImportedTypeViaInference Result: $output")

        assertTrue(output.contains("InferMarker"), "Output should contain 'InferMarker', but was: $output")
    }

    @Test
    fun testDeepTransitiveChain() = runBlocking {
        // A -> B -> C -> D, each contributing a helper, exercised via function calls. Verifies that
        // a deep transitive @file:DependsOnScript chain is fully imported without inlining sources.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        loadedScripts["chainD"] = Script().apply {
            id = "chainD"
            person = "person1"
            source = """
                fun helperD() = "D"
            """.trimIndent()
        }
        loadedScripts["chainC"] = Script().apply {
            id = "chainC"
            person = "person1"
            source = """
                @file:DependsOnScript("chainD")
                fun helperC() = helperD() + "C"
            """.trimIndent()
        }
        loadedScripts["chainB"] = Script().apply {
            id = "chainB"
            person = "person1"
            source = """
                @file:DependsOnScript("chainC")
                fun helperB() = helperC() + "B"
            """.trimIndent()
        }
        val scriptA = Script().apply {
            id = "chainA"
            person = "person1"
            source = """
                @file:DependsOnScript("chainB")
                render {
                    text(helperB() + "A")
                }
            """.trimIndent()
        }
        loadedScripts["chainA"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] DeepTransitiveChain Result: $output")

        assertTrue(output.contains("DCBA"), "Output should contain 'DCBA', but was: $output")
    }

    @Test
    fun testDiamondDependency() = runBlocking {
        // Diamond: A -> B and A -> C, with both B and C -> D. D must be imported exactly once
        // (no duplicate-declaration error) and usable through both branches.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        loadedScripts["diaD"] = Script().apply {
            id = "diaD"
            person = "person1"
            source = """
                fun helperD() = "Diamond"
            """.trimIndent()
        }
        loadedScripts["diaB"] = Script().apply {
            id = "diaB"
            person = "person1"
            source = """
                @file:DependsOnScript("diaD")
                fun helperB() = helperD() + "-B"
            """.trimIndent()
        }
        loadedScripts["diaC"] = Script().apply {
            id = "diaC"
            person = "person1"
            source = """
                @file:DependsOnScript("diaD")
                fun helperC() = helperD() + "-C"
            """.trimIndent()
        }
        val scriptA = Script().apply {
            id = "diaA"
            person = "person1"
            source = """
                @file:DependsOnScript("diaB")
                @file:DependsOnScript("diaC")
                render {
                    text(helperB() + helperC())
                }
            """.trimIndent()
        }
        loadedScripts["diaA"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] DiamondDependency Result: $output")

        assertTrue(output.contains("Diamond-B"), "Output should contain 'Diamond-B', but was: $output")
        assertTrue(output.contains("Diamond-C"), "Output should contain 'Diamond-C', but was: $output")
    }

    @Test
    fun testReifiedInlineSerializationAcrossScripts() = runBlocking {
        // Reproduces the actual reported failure: an inline reified function is declared in an
        // imported script (like StructuredDeepSeek.prompt<T>) and is called from the *main* script
        // with a @Serializable type that is declared in the main script. The serializer<T>() call
        // is inlined into the main script, so at runtime kotlinx.serialization must be able to find
        // the generated serializer for the main-script type.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "reifiedDep1"
            person = "person1"
            source = """
                import kotlinx.serialization.encodeToString

                inline fun <reified T : Any> encodeRoundTrip(value: T): T =
                    Json.decodeFromString(Json.encodeToString(value))
            """.trimIndent()
        }
        loadedScripts["reifiedDep1"] = scriptB

        val scriptA = Script().apply {
            id = "reifiedMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("reifiedDep1")
                import kotlinx.serialization.encodeToString

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = ""
                )

                render {
                    val info = encodeRoundTrip(DeepSeekWordInfo(original = "hello", translation = "hola"))
                    text(Json.encodeToString(info))
                }
            """.trimIndent()
        }
        loadedScripts["reifiedMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] ReifiedInlineSerializationAcrossScripts Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the round-tripped script-defined class, but was: $output"
        )
    }

    @Test
    fun testPackagedScriptSerialization() = runBlocking {
        // Faithful reproduction of the user's setup: the main script declares its own `package`,
        // depends on a script in a *different* package that exposes an inline reified serialization
        // helper, and serializes a @Serializable class declared in the main script's package.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "pkgDep1"
            person = "person1"
            source = """
                package chat.hitown.deepseek

                import kotlinx.serialization.encodeToString

                inline fun <reified T : Any> encodeRoundTrip(value: T): T =
                    Json.decodeFromString(Json.encodeToString(value))
            """.trimIndent()
        }
        loadedScripts["pkgDep1"] = scriptB

        val scriptA = Script().apply {
            id = "pkgMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("pkgDep1")

                package chat.hitown.language

                import kotlinx.serialization.encodeToString
                import chat.hitown.deepseek.encodeRoundTrip

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = ""
                )

                render {
                    val info = encodeRoundTrip(DeepSeekWordInfo(original = "hello", translation = "hola"))
                    text(Json.encodeToString(info))
                }
            """.trimIndent()
        }
        loadedScripts["pkgMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] PackagedScriptSerialization Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the serialized packaged script-defined class, but was: $output"
        )
    }

    @Test
    fun testSuspendInlineReifiedExtensionAcrossScripts() = runBlocking {
        // Closest reproduction of the real failing shape: a `suspend inline reified` *extension*
        // function on a class is declared in an imported, packaged script (like
        // StructuredDeepSeek.prompt<T>) and called from within runBlocking in the main script with a
        // @Serializable type declared in the main script.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "ssDep1"
            person = "person1"
            source = """
                package chat.hitown.deepseek

                import kotlinx.serialization.encodeToString

                class StructuredDeepSeek

                suspend inline fun <reified T : Any> StructuredDeepSeek.prompt(value: T): T? =
                    Json.encodeToString(value).let { Json.decodeFromString<T>(it) }
            """.trimIndent()
        }
        loadedScripts["ssDep1"] = scriptB

        val scriptA = Script().apply {
            id = "ssMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("ssDep1")

                package chat.hitown.language

                import kotlinx.coroutines.runBlocking
                import kotlinx.serialization.encodeToString
                import chat.hitown.deepseek.StructuredDeepSeek
                import chat.hitown.deepseek.prompt

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = "",
                    val sentences: List<String> = emptyList()
                )

                runBlocking {
                    val deepSeek = StructuredDeepSeek()
                    val info = deepSeek.prompt(DeepSeekWordInfo(original = "hello", translation = "hola"))!!
                    render {
                        text(Json.encodeToString(info))
                    }
                }
            """.trimIndent()
        }
        loadedScripts["ssMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] SuspendInlineReifiedExtensionAcrossScripts Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the serialized class, but was: $output"
        )
    }

    @Test
    fun testFaithfulStructuredDeepSeekReproduction() = runBlocking {
        // Mirrors the real StructuredDeepSeek imported script as closely as possible without network:
        // private @Serializable response classes, a top-level `private val json = Json { ... }`, a
        // star import of kotlinx.serialization, and a `suspend inline reified` extension `prompt<T>`
        // that encodes the example and decodes a result string. The @Serializable DeepSeekWordInfo is
        // declared in the main (packaged) script and passed as the reified type.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptB = Script().apply {
            id = "dsDep1"
            person = "person1"
            source = """
                package chat.hitown.deepseek

                import kotlinx.serialization.*

                @Serializable
                private data class ChatMessage(
                    val role: String,
                    val content: String
                )

                @Serializable
                private data class DeepSeekResponse(
                    val id: String? = null,
                    val choices: List<ChatMessage>
                )

                private val json = Json {
                    encodeDefaults = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }

                class StructuredDeepSeek(val apiKey: String = "key") {
                    suspend fun promptString(userPrompt: String, exampleJson: String): String? = exampleJson
                }

                suspend inline fun <reified T : Any> StructuredDeepSeek.prompt(
                    userPrompt: String,
                    exampleJson: T
                ): T? = promptString(
                    userPrompt,
                    Json.encodeToString(exampleJson)
                )?.let {
                    Json.decodeFromString(it)
                }
            """.trimIndent()
        }
        loadedScripts["dsDep1"] = scriptB

        val scriptA = Script().apply {
            id = "dsMain1"
            person = "person1"
            source = """
                @file:DependsOnScript("dsDep1")

                package chat.hitown.language

                import kotlinx.coroutines.runBlocking
                import kotlinx.serialization.encodeToString
                import chat.hitown.deepseek.StructuredDeepSeek
                import chat.hitown.deepseek.prompt

                @Serializable
                data class DeepSeekWordInfo(
                    val original: String = "",
                    val translation: String = "",
                    val sentences: List<String> = emptyList(),
                    val englishTranslations: List<String> = emptyList()
                )

                runBlocking {
                    val deepSeek = StructuredDeepSeek()
                    val example = DeepSeekWordInfo(original = "hello", translation = "hola")
                    val dsRes = deepSeek.prompt(userPrompt = "p", exampleJson = example)!!
                    render {
                        text(Json.encodeToString(dsRes))
                    }
                }
            """.trimIndent()
        }
        loadedScripts["dsMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] FaithfulStructuredDeepSeekReproduction Result: $output")

        assertTrue(
            output.contains("\"original\":\"hello\"") && output.contains("\"translation\":\"hola\""),
            "Output should contain the serialized class, but was: $output"
        )
    }

    @Test
    fun testNestedScriptDefinedSerializableClasses() = runBlocking {
        // Mirrors the user's main-script FlowState/WordPracticeData: a @Serializable class whose
        // generated serializer references *another* script-defined @Serializable class (nested in a
        // List). Serialized via Json.encodeToString(flowState) and round-tripped back, like the real
        // script's `Json.encodeToString(initialState)` / `decodeFromString<FlowState>(data)`.
        val loadedScripts = mutableMapOf<String, Script>()
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }

        val scriptA = Script().apply {
            id = "nestedMain1"
            person = "person1"
            source = """
                package chat.hitown.language

                import kotlinx.serialization.encodeToString
                import kotlinx.serialization.decodeFromString

                @Serializable
                data class WordPracticeData(
                    val word: String,
                    val translation: String,
                    val sentences: List<String>,
                    val audioUrls: List<String>,
                    val englishSentences: List<String>
                )

                @Serializable
                data class FlowState(
                    val targetLanguage: String,
                    val wordsData: List<WordPracticeData>,
                    val currentWord: Int = 0,
                    val currentSentence: Int = -1,
                    val showSummaryFirst: Boolean = false,
                    val showReasoningFirst: Boolean = false
                )

                render {
                    val state = FlowState(
                        targetLanguage = "Burmese",
                        wordsData = listOf(
                            WordPracticeData(
                                word = "apple",
                                translation = "ပန်းသီး",
                                sentences = listOf("s1"),
                                audioUrls = listOf("u1"),
                                englishSentences = listOf("e1")
                            )
                        ),
                        showReasoningFirst = true
                    )
                    val parser = Json { ignoreUnknownKeys = true }
                    val json = Json.encodeToString(state)
                    val restored = parser.decodeFromString<FlowState>(json)
                    text(Json.encodeToString(restored))
                }
            """.trimIndent()
        }
        loadedScripts["nestedMain1"] = scriptA

        val runner = RunScript(scriptA, null, null, useCache = false)
        ScriptWithMavenDepsConfiguration.scriptLoader = { id -> loadedScripts[id] }
        val result = runner.run(null)

        val output = result.content?.filterIsInstance<StoryContent.Text>()?.joinToString("") { it.text } ?: ""
        println("[DEBUG_LOG] NestedScriptDefinedSerializableClasses Result: $output")

        assertTrue(
            output.contains("\"word\":\"apple\"") && output.contains("\"targetLanguage\":\"Burmese\""),
            "Output should contain the nested serialized classes, but was: $output"
        )
    }
}
