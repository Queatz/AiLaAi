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

    // Known limitation: KT-86352 (https://youtrack.jetbrains.com/issue/KT-86352).
    // Under K2 in Kotlin 2.4.0 the compiler crashes ("Expected FirResolvedTypeRef with ConeKotlinType
    // but was FirUserTypeRefImpl") when the importing script references a type declared in an
    // imported (@file:DependsOnScript) script from an *explicit type position* (here
    // `val d: List<WordPracticeData>`). The fix is planned for Kotlin 2.4.10. This test asserts the
    // desired (post-fix) behavior and is @Ignore'd until that version is adopted; remove @Ignore
    // once the project is on Kotlin 2.4.10+. The same script using inference
    // (see testImportedTypeViaInference) works today.
    @Ignore("KT-86352: explicit imported-script type position crashes K2 FIR until Kotlin 2.4.10")
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
}
