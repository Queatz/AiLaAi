package app.scripts

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.PageTopBar
import app.ailaai.api.deleteScript
import app.ailaai.api.runScript
import app.ailaai.api.updateScript
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import appString
import application
import bulletedString
import com.queatz.db.RunScriptBody
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import components.IconButton
import components.Loading
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import stories.StoryContents

@Composable
fun ScriptsPage(
    nav: ScriptsNav,
    onUpdate: (Script) -> Unit,
    onScriptDeleted: (Script) -> Unit,
) {
    val scope = rememberCoroutineScope()

    when (nav) {
        is ScriptsNav.None -> Unit
        is ScriptsNav.Script -> {
            var script by remember(nav.script) { mutableStateOf(nav.script) }
            var isLoading by remember { mutableStateOf(false) }
            var editedScript by remember(script) { mutableStateOf<String?>(null) }
            var edited by remember(script) { mutableStateOf(false) }
            var isSaving by remember(script) { mutableStateOf(false) }
            var showResultPanel by remember(script) { mutableStateOf(false) }
            var isRunningScript by remember(script) { mutableStateOf(false) }
            var runScriptData by remember(script) { mutableStateOf<String?>(null) }
            var scriptResult by remember(script) { mutableStateOf<ScriptResult?>(null) }
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }

            menuTarget?.let {
                Menu(
                    onDismissRequest = { menuTarget = null },
                    target = it,
                ) {
                    item(
                        // todo: translate
                        title = "Rename",
                        onClick = {
                            menuTarget = null
                            scope.launch {
                                val name = inputDialog(
                                    // todo: translate
                                    title = "Script name",
                                    confirmButton = application.appString { update },
                                    defaultValue = script.name.orEmpty()
                                )

                                if (name != null) {
                                    api.updateScript(
                                        id = script.id!!,
                                        script = Script(name = name)
                                    ) {
                                        onUpdate(it)
                                    }
                                }
                            }
                        }
                    )
                    item(
                        // todo: translate
                        title = "Delete",
                        onClick = {
                            menuTarget = null
                            scope.launch {
                                val proceed = dialog(
                                    // todo: translate
                                    title = "Delete this script?",
                                    // todo: translate
                                    confirmButton = "Yes, delete"
                                )
                                if (proceed == true) {
                                    api.deleteScript(script.id!!) {
                                        onScriptDeleted(script)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            val runScript = remember(script) {
                { scriptId: String, data: String? ->
                    scope.launch {
                        api.runScript(scriptId, RunScriptBody(data = data)) {
                            scriptResult = it
                            isRunningScript = false
                        }
                    }
                }
            }

            if (isLoading) {
                Loading()
            } else {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flex(1)
                        height(0.r)
                    }
                }) {
                    Div({
                        style {
                            flex(1)
                            width(0.r)
                            height(100.percent)
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            overflowX("hidden")
                            overflowY("auto")
                        }
                    }) {
                        MonacoEditor(
                            initialValue = script.source.orEmpty(),
                            onValueChange = {
                                editedScript = it
                                edited = editedScript != script.source
                            },
                            styles = {
                                margin(0.r, 1.r, 1.r, 1.r)
                                flexGrow(1)
                            }
                        )
                    }
                    if (showResultPanel) {
                        Div({
                            style {
                                flexShrink(0)
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                overflowX("hidden")
                                overflowY("auto")
                                width(24.r)
                                height(100.percent)
                            }
                        }) {
                            if (isRunningScript) {
                                Loading()
                            } else {
                                scriptResult?.content?.let {
                                    Div({
                                        classes(Styles.cardContent)
                                    }) {
                                        StoryContents(
                                            storyContent = it,
                                            onButtonClick = { script, data -> runScript(script, data) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                PageTopBar(
                    // todo: translate
                    title = script.name ?: "New script",
                    useMinHeight = true,
                    description = bulletedString(
                        script.categories?.firstOrNull(),
                        script.description
                    ),
                    actions = {
                        if (edited) {
                            Button(
                                attrs = {
                                    classes(Styles.button)

                                    onClick {
                                        scope.launch {
                                            isSaving = true
                                            api.updateScript(
                                                id = script.id!!,
                                                script = Script(source = editedScript)
                                            ) {
                                                onUpdate(it)
                                            }
                                            isSaving = false
                                        }
                                    }

                                    if (isSaving) {
                                        disabled()
                                    }
                                }
                            ) {
                                Text(appString { save })
                            }
                        } else {
                            IconButton(
                                name = "play_arrow",
                                // todo: translate
                                title = "Run script",
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                showResultPanel = true
                                runScriptData = null
                                isRunningScript = true
                                runScript(script.id!!, null)
                            }
                        }
                        IconButton(
                            name = "help_outline",
                            // todo: translate
                            title = "Help",
                            styles = {
                                marginLeft(.5.r)
                            }
                        ) {
                            scope.launch {
                                dialog(
                                    title = "Kotlin Scripts",
                                    cancelButton = null,
                                    content = {
                                        Div(
                                            {
                                                style {
                                                    whiteSpace("pre-wrap")
                                                }
                                            }
                                        ) {
                                            Text(
                                                """
                                                Scripts are written in Kotlin. You may want to use your favorite code editor to write scripts, as you will need to add all import statements. 
                                                
                                                The following variables are passed to into scripts:
                                                
                                                self: String // The id of this script
                                                me: Person? // The current user, if signed in, null if signed out
                                                data: String? // Data passed to the script, if any
                                                
                                                Person has the following fields: id, name, photo, language, utcOffset, seen
                                                
                                                The following are functions passed into scripts: render, http
                                                
                                                Rendering content:
                                                
                                                render {
                                                    section("<Title>")
                                                    text("<Text>")
                                                    button(
                                                        text = "<Button text>",
                                                        script = "<Script ID to run>",
                                                        data = "<Data passed to the script>",
                                                    )
                                                    photo("<Url>", <Aspect ratio>?) // Must start with /static/
                                                }
                                                
                                                Networking:
                                                
                                                Ktor's HttpClient is used for simple networking. Learn more at ktor.io.
                                                
                                                http<Any type here>("<url>")
                                                http.post<Response type here, Request type here>("<url>", body = <Any object here>)
                                                
                                                KotlinX Serialization is available. The recommendation is to use @Serializable data classes.
                                                
                                                @Serializable
                                                data class Request(val data: String)
                                                
                                                @Serializable
                                                data class Response(val data: String)
                                                
                                                val post = http.post<Response, Request>("<url>", body = Request(""))
                                                val get = http<Response>("<url>")
                                                val get: Response = http("<url>") // or this
                                                val get: Response = http("<url>", headers = mapOf("Authorization" to "<token>")) // set headers
                                                
                                                Other common response types are: String, HttpStatusCode, JsonObject
                                                
                                                Advanced:
                                                
                                                You can depend on packages from Maven Repositories.
                                                
                                                @file:Repository("<maven url>")
                                                @file:DependsOn("<package>")
                                                
                                                Learn more at kotlinlang.org
                                                """.trimIndent()
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(
                            name = "more_vert",
                            // todo: translate
                            title = "Menu",
                            styles = {
                                marginLeft(.5.r)
                            }
                        ) {
                            menuTarget =
                                if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
                        }
                    }
                )
            }
        }
    }
}
