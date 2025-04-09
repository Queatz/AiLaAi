package app.scripts

import Styles
import aiScript
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.PageTopBar
import app.ailaai.api.categories
import app.ailaai.api.deleteScript
import app.ailaai.api.runScript
import app.ailaai.api.updateScript
import app.ailaai.shared.resources.ScriptsResources
import app.dialog.categoryDialog
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import app.messaages.inList
import appString
import application
import bulletedString
import com.queatz.db.AiScriptRequest
import com.queatz.db.RunScriptBody
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import com.queatz.db.asGeo
import components.IconButton
import components.LinkifyText
import components.Loading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAnimationFrame
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
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
            var isAiScriptGenerating by remember(script) { mutableStateOf(false) }
            var aiScript by remember(script) { mutableStateOf<String?>(null) }
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }
            var aiJob by remember { mutableStateOf<Job?>(null) }

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
                        title = "Category",
                        onClick = {
                            menuTarget = null
                            scope.launch {
                                api.categories(
                                    // todo: might need to not require geo
                                    geo = application.me.value?.geo?.asGeo() ?: return@launch
                                ) { categories ->
                                    val category = categoryDialog(
                                        categories = categories
                                    )

                                    if (category != null) {
                                        api.updateScript(
                                            id = script.id!!,
                                            script = Script(categories = category.inList())
                                        ) {
                                            onUpdate(it)
                                        }
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
                { scriptId: String, data: String?, input: Map<String, String?>, useCache: Boolean? ->
                    scope.launch {
                        api.runScript(
                            id = scriptId,
                            data = RunScriptBody(
                                data = data,
                                input = input,
                                useCache = useCache
                            )
                        ) {
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
                            initialValue = aiScript ?: script.source.orEmpty(),
                            onValueChange = {
                                editedScript = it
                                edited = it != script.source && !(it.isBlank() && script.source!!.isBlank())
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
                                            content = it,
                                            onButtonClick = { script, data, input ->
                                                runScript(
                                                    script,
                                                    data,
                                                    input,
                                                    true
                                                )
                                            },
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
                        script.description,
                        script.id
                    ),
                    actions = {
                        if (aiScript != null && editedScript == aiScript) {
                            IconButton(
                                name = "undo",
                                // todo: translate
                                title = "Undo AI changes",
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                aiScript = null
                            }
                        }
                        if (edited) {
                            Button(
                                attrs = {
                                    classes(Styles.button)

                                    style {
                                        marginLeft(.5.r)
                                    }

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
                                title = "Run script (Hold SHIFT to skip cache)",
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                showResultPanel = true
                                runScriptData = null
                                isRunningScript = true
                                runScript(script.id!!, null, emptyMap(), !it.shiftKey)
                            }
                            IconButton(
                                name = "auto_awesome",
                                // todo: translate
                                title = "Code with AI",
                                isLoading = isAiScriptGenerating,
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                if (isAiScriptGenerating) {
                                    scope.launch {
                                        val result = dialog(
                                            // todo: translate
                                            title = "Stop script generation?",
                                            // todo: translate
                                            confirmButton = "Yes, stop"
                                        )

                                        if (result == true) {
                                            aiJob?.cancel()
                                            isAiScriptGenerating = false
                                        }
                                    }
                                } else {
                                    aiJob = scope.launch {
                                        val prompt = inputDialog(
                                            // todo: translate
                                            title = "AI Prompt",
                                            // todo: translate
                                            confirmButton = "Send",
                                            singleLine = false,
                                            // todo: translate
                                            placeholder = if ((editedScript
                                                    ?: script.source!!).isBlank()
                                            ) "Create a script that..." else "Modify this script to...",
                                        )

                                        if (prompt != null) {
                                            isAiScriptGenerating = true
                                            api.aiScript(
                                                request = AiScriptRequest(
                                                    prompt = prompt,
                                                    script = (editedScript ?: script.source!!)
                                                ),
                                                onError = { error ->
                                                    if (error !is CancellationException) {
                                                        scope.launch {
                                                            dialog(
                                                                // todo: translate
                                                                title = "Something went wrong",
                                                                cancelButton = null,
                                                                content = {
                                                                    // todo: translate
                                                                    Text("Please try again.")
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                aiScript = it.code
                                            }
                                            isAiScriptGenerating = false
                                        }

                                        aiJob = null
                                    }
                                }
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
                                            LinkifyText(ScriptsResources.documentation)
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
