package com.queatz.scripts

import ScriptRender
import ScriptWithMavenDeps
import com.queatz.db.Person
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import com.queatz.db.StoryContent
import kotlin.reflect.full.createType
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate


class RunScript(private val script: Script, private val data: String?) {
    fun run(person: Person?): ScriptResult {
        var content: List<StoryContent>? = null

        val result = BasicJvmScriptingHost().eval(
            "@file:Suppress(\"PROVIDED_RUNTIME_TOO_LOW\")\n\n${script.source!!}".toScriptSource(),
            createJvmCompilationConfigurationFromTemplate<ScriptWithMavenDeps> {
                providedProperties(
                    "me" to Person::class.createType(nullable = true),
                    "self" to String::class.createType(),
                    "render" to ScriptRender::class.createType(),
                    "http" to ScriptHttp::class.createType(),
                    "data" to String::class.createType(nullable = true),
                )
            },
            createJvmEvaluationConfigurationFromTemplate<ScriptWithMavenDeps> {
                providedProperties(
                    "me" to person?.let { Person().apply {
                        id = it.id
                        name = it.name
                        photo = it.photo
                        language = it.language
                        utcOffset = it.utcOffset
                        seen = it.seen
                    } },
                    "self" to script.id!!,
                    "render" to ScriptRender { content = it },
                    "http" to ScriptHttp(),
                    "data" to this@RunScript.data
                )
            }
        )

        println(result.toString())

        val resultError = (result as? ResultWithDiagnostics.Success)?.value?.returnValue as? ResultValue.Error

        return if (result.isError() || resultError != null) {
            ScriptResult(
                content = listOf(
                    StoryContent.Text(
                        "Script error: ${resultError?.let { "$it\n\n" }}${
                            result.reports.joinToString("\n")
                        }"
                    )
                )
            )
        } else {
            ScriptResult(content = content)
        }
    }
}
