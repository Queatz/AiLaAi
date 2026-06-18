package com.queatz.scripts

import com.queatz.db.Script
import com.queatz.db.StoryContent
import com.queatz.scripts.RunScript
import com.queatz.scripts.ScriptWithMavenDepsConfiguration
import kotlinx.coroutines.runBlocking
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
}
