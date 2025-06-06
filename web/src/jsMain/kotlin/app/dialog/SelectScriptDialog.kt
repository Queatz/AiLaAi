package app.dialog

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.scripts
import app.scripts.MonacoEditorState
import appString
import appText
import application
import bulletedString
import com.queatz.db.Script
import components.LinkifyText
import components.Loading
import components.SearchField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import com.queatz.widgets.widgets.ScriptData
import json

suspend fun selectScriptDialog(
    scope: CoroutineScope,
    onScriptSelected: (scriptId: String, scriptData: String) -> Unit
) {
    dialog(
        title = application.appString { scripts },
        confirmButton = null,
        cancelButton = application.appString { cancel },
        content = { resolve ->
            var scripts by remember { mutableStateOf<List<Script>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var search by remember { mutableStateOf("") }

            var hasMore by remember { mutableStateOf(true) }
            var isLoadingMore by remember { mutableStateOf(false) }
            var offset by remember { mutableStateOf(0) }
            val limit = 20

            fun loadMore() {
                if (!hasMore || isLoadingMore) return
                isLoadingMore = true
                scope.launch {
                    api.scripts(
                        search = search.notBlank,
                        offset = offset,
                        limit = limit
                    ) {
                        if (offset == 0) {
                            scripts = it
                        } else {
                            scripts = (scripts + it).distinctBy { it.id }
                            offset += limit
                        }
                        hasMore = it.size >= limit
                        offset += limit
                    }
                    isLoadingMore = false
                    isLoading = false
                }
            }

            LaunchedEffect(search) {
                offset = 0
                isLoading = true
                hasMore = true
                loadMore()
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    width(32.r)
                    height(24.r)
                }
            }) {
                SearchField(
                    value = search,
                    placeholder = appString { this.search },
                    shadow = false,
                    styles = {
                        margin(.5.r)
                    },
                    onValue = {
                        search = it
                    }
                )

                if (isLoading) {
                    Loading()
                } else if (scripts.isNotEmpty()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            gap(.5.r)
                            padding(0.r, .5.r)
                            property("overflow-y", "auto")
                            flex(1)
                        }
                    }) {
                        scripts.forEach { script ->
                            Div({
                                classes(AppStyles.scriptItem)

                                onClick {
                                    scope.launch {
                                        val scriptData = json.encodeToString(ScriptData(script = script.id))
                                        onScriptSelected(script.id!!, scriptData)
                                        resolve(false)
                                    }
                                }
                            }) {
                                Div({
                                    style {
                                        fontSize(18.px)
                                        fontWeight("bold")
                                        marginBottom(.5.r)
                                    }
                                }) {
                                    Text(script.name?.notBlank ?: appString { newScript })
                                }
                                Div({
                                    style {
                                        opacity(.5)
                                    }
                                }) {
                                    Text(
                                        bulletedString(
                                            script.author?.name?.let { "By $it" },
                                            script.categories?.firstOrNull(),
                                            script.id!!
                                        )
                                    )
                                }
                                script.description?.notBlank?.let { description ->
                                    Div({
                                        style {
                                            marginTop(.5.r)
                                            overflow("auto")
                                        }
                                    }) {
                                        LinkifyText(description)
                                    }
                                }
                            }
                        }
                    }
                } else if (search.isNotBlank()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        appText { noScripts }
                    }
                } else {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        appText { noScripts }
                    }
                }
            }
        }
    )
}

suspend fun addScriptDependencyDialog(
    scope: CoroutineScope,
    state: MonacoEditorState,
    editedScript: String?,
    script: Script
) {
    dialog(
        title = application.appString { dependOnScript },
        confirmButton = null,
        cancelButton = application.appString { cancel },
        content = { resolve ->
            var scripts by remember { mutableStateOf<List<Script>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var search by remember { mutableStateOf("") }

            var hasMore by remember { mutableStateOf(true) }
            var isLoadingMore by remember { mutableStateOf(false) }
            var offset by remember { mutableStateOf(0) }
            val limit = 20

            fun loadMore() {
                if (!hasMore || isLoadingMore) return
                isLoadingMore = true
                scope.launch {
                    api.scripts(
                        search = search.notBlank,
                        offset = offset,
                        limit = limit
                    ) {
                        val newScripts = it.filter { it.id != script.id }
                        if (offset == 0) {
                            scripts = newScripts
                        } else {
                            scripts = (scripts + newScripts).distinctBy { it.id }
                            offset += limit
                        }
                        hasMore = newScripts.size >= limit
                        offset += limit
                    }
                    isLoadingMore = false
                    isLoading = false
                }
            }

            LaunchedEffect(search) {
                offset = 0
                isLoading = true
                hasMore = true
                loadMore()
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    width(32.r)
                    height(24.r)
                }
            }) {
                SearchField(
                    value = search,
                    placeholder = appString { this.search },
                    shadow = false,
                    styles = {
                        margin(.5.r)
                    },
                    onValue = {
                        search = it
                    }
                )

                if (isLoading) {
                    Loading()
                } else if (scripts.isNotEmpty()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            gap(.5.r)
                            padding(0.r, .5.r)
                            property("overflow-y", "auto")
                            flex(1)
                        }
                    }) {
                        scripts.forEach { selectedScript ->
                            Div({
                                classes(AppStyles.scriptItem)

                                onClick {
                                    scope.launch {
                                        val currentSource = editedScript ?: script.source.orEmpty()
                                        val description = if (selectedScript.description.isNullOrBlank()) {
                                            ""
                                        } else {
                                            "\n/* ${selectedScript.description.orEmpty()}\n */\n"
                                        }
                                        val dependsOnLine =
                                            "$description@file:DependsOnScript(\"${selectedScript.id}\") // ${selectedScript.name}"

                                        // Check if the script already has a @file:DependsOnScript annotation
                                        val newSource = if (currentSource.contains("@file:DependsOnScript")) {
                                            // Insert the new annotation after the last @file:DependsOnScript annotation
                                            val lines = currentSource.lines()
                                            val lastAnnotationIndex =
                                                lines.indexOfLast { it.contains("@file:DependsOnScript") }

                                            if (lastAnnotationIndex >= 0) {
                                                val updatedLines = lines.toMutableList()
                                                updatedLines.add(lastAnnotationIndex + 1, dependsOnLine)
                                                updatedLines.joinToString("\n")
                                            } else {
                                                "$dependsOnLine\n$currentSource"
                                            }
                                        } else {
                                            "$dependsOnLine\n$currentSource"
                                        }

                                        state.setValue(newSource)
                                        resolve(false)
                                    }
                                }
                            }) {
                                Div({
                                    style {
                                        fontSize(18.px)
                                        fontWeight("bold")
                                        marginBottom(.5.r)
                                    }
                                }) {
                                    Text(selectedScript.name?.notBlank ?: appString { newScript })
                                }
                                Div({
                                    style {
                                        opacity(.5)
                                    }
                                }) {
                                    Text(
                                        bulletedString(
                                            selectedScript.author?.name?.let { "By $it" },
                                            selectedScript.categories?.firstOrNull(),
                                            selectedScript.id!!
                                        )
                                    )
                                }
                                selectedScript.description?.notBlank?.let { description ->
                                    Div({
                                        style {
                                            marginTop(.5.r)
                                            overflow("auto")
                                        }
                                    }) {
                                        LinkifyText(description)
                                    }
                                }
                            }
                        }
                    }
                } else if (search.isNotBlank()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        appText { noScripts }
                    }
                } else {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        appText { noScripts }
                    }
                }
            }
        }
    )
}
