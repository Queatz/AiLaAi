package app.group

import LocalConfiguration
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.*
import app.cards.MapList
import app.components.Empty
import app.components.FlexInput
import app.dialog.editTaskDialog
import app.dialog.selectCardDialog
import app.dialog.selectScriptDialog
import app.nav.CardItem
import appString
import application
import com.queatz.db.Card
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import com.queatz.db.RunScriptBody
import com.queatz.db.ScriptResult
import components.Content
import components.IconButton
import components.Loading
import components.Markdown
import components.SearchField
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLIFrameElement
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Text
import r
import saves
import stories.StoryContents
import web.cssom.HtmlAttributes.Companion.list
import com.queatz.db.GroupContent as GroupContentModel

@Composable
fun GroupContent(
    group: GroupExtended,
    onUpdated: (GroupExtended) -> Unit,
    setTitle: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val content = group.group?.content?.let {
        try {
            json.decodeFromString<GroupContentModel>(it)
        } catch (e: Exception) {
            null
        }
    }

    if (content == null || content is GroupContentModel.None) {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                alignItems(AlignItems.Stretch)
                justifyContent(JustifyContent.Center)
                flex(1)
                overflow("auto")
                padding(1.r)
                gap(1.r)
            }
        }) {
            FeatureButton(
                icon = "edit",
                title = "Notes",
                description = "Add simple notes"
            ) {
                scope.launch {
                    api.updateGroup(
                        group.group!!.id!!,
                        Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Text("")))
                    ) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
            FeatureButton(
                icon = "check_box",
                title = "Tasks",
                description = "Show the group's tasks"
            ) {
                scope.launch {
                    api.updateGroup(
                        group.group!!.id!!,
                        Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Tasks()))
                    ) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
            FeatureButton(
                icon = "description",
                title = "Page",
                description = "Show a page"
            ) {
                scope.launch {
                    selectCardDialog(configuration) { card ->
                        scope.launch {
                            api.updateGroup(
                                group.group!!.id!!,
                                Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Card(card.id)))
                            ) {
                                onUpdated(group.apply { this.group!!.content = it.content })
                            }
                        }
                    }
                }
            }
            FeatureButton(
                icon = "code",
                title = "Script",
                description = "Show a script"
            ) {
                scope.launch {
                    selectScriptDialog(scope) { scriptId, scriptData ->
                        scope.launch {
                            api.updateGroup(
                                group.group!!.id!!,
                                Group(
                                    content = json.encodeToString<GroupContentModel>(
                                        GroupContentModel.Script(
                                            scriptId,
                                            scriptData
                                        )
                                    )
                                )
                            ) {
                                onUpdated(group.apply { this.group!!.content = it.content })
                            }
                        }
                    }
                }
            }
            FeatureButton(
                icon = "public",
                title = "Website",
                description = "Show a website"
            ) {
                scope.launch {
                    api.updateGroup(
                        group.group!!.id!!,
                        Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Website("")))
                    ) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
        }
    } else {
        key(group.group?.id) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    height(100.percent)
                }
            }) {
                when (content) {
                    is GroupContentModel.Text -> {
                        var editing by remember { mutableStateOf(content.text.isNullOrBlank()) }
                        if (editing) {
                            var text by remember { mutableStateOf(content.text ?: "") }
                            FlexInput(
                                value = text,
                                onChange = {
                                    text = it
                                },
                                onSubmit = {
                                    api.updateGroup(
                                        group.group!!.id!!,
                                        Group(
                                            content = json.encodeToString<GroupContentModel>(
                                                GroupContentModel.Text(
                                                    text
                                                )
                                            )
                                        )
                                    ) {
                                        onUpdated(group.apply { this.group!!.content = it.content })
                                        editing = false
                                    }
                                    true
                                },
                                autoFocus = true,
                                onDismissRequest = { editing = false },
                                showButtons = true
                            )
                        } else {
                            Div({
                                style {
                                    padding(1.r)
                                }
                                onClick { editing = true }
                            }) {
                                if (content.text.isNullOrBlank()) {
                                    Empty { Text("No text.") }
                                } else {
                                    Markdown(content.text!!)
                                }
                            }
                        }
                    }

                    is GroupContentModel.Tasks -> {
                        var cards by remember { mutableStateOf<List<Card>?>(null) }
                        fun reload() {
                            scope.launch {
                                api.groupCards(group.group!!.id!!) {
                                    cards = it
                                }
                            }
                        }
                        LaunchedEffect(group) {
                            reload()
                        }

                        if (cards == null) {
                            Loading()
                        } else {
                            var search by remember { mutableStateOf("") }
                            val filteredCards = remember(cards, search) {
                                val list = if (search.isBlank()) cards!! else cards!!.filter {
                                    it.name?.contains(search, ignoreCase = true) == true ||
                                            it.content?.contains(search, ignoreCase = true) == true ||
                                            it.categories?.any { it.contains(search, ignoreCase = true) } == true ||
                                            it.task?.let { task ->
                                                task.status?.contains(search, ignoreCase = true) == true ||
                                                        task.fields?.any {
                                                            it.value.contains(
                                                                search,
                                                                ignoreCase = true
                                                            )
                                                        } == true
                                            } == true
                                }
                                list.sortedWith(
                                    compareBy<Card> { it.task?.done ?: false }
                                        .thenByDescending { it.createdAt }
                                )
                            }

                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    alignItems(AlignItems.Center)
                                    gap(.5.r)
                                }
                            }) {
                                Div({
                                    style { flex(1) }
                                }) {
                                    FlexInput(
                                        value = search,
                                        onChange = { search = it },
                                        placeholder = appString { this.search },
                                        onDismissRequest = {
                                            search = ""
                                        }
                                    )
                                }
                                IconButton("add", appString { newTask }, styles = {
                                    marginRight(.5.r)
                                }) {
                                    scope.launch {
                                        editTaskDialog(group.group!!.id!!, allCards = cards) {
                                            reload()
                                        }
                                    }
                                }
                            }

                            if (filteredCards.isEmpty()) {
                                Empty { Text(if (search.isBlank()) "No tasks." else "No results.") }
                            } else {
                                MapList(
                                    cards = filteredCards,
                                    allCards = cards,
                                    showPhoto = false,
                                    people = group.members?.mapNotNull { it.person },
                                    groupId = group.group!!.id!!,
                                    onUpdated = { reload() },
                                    styles = {
                                        marginTop(1.r)
                                    }
                                ) { card ->
                                    scope.launch {
                                        editTaskDialog(group.group!!.id!!, card, cards) {
                                            reload()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is GroupContentModel.Card -> {
                        content.cardId?.let { cardId ->
                            var card by remember { mutableStateOf<Card?>(null) }
                            LaunchedEffect(cardId) {
                                api.card(cardId) {
                                    card = it
                                    setTitle(it.name)
                                }
                            }

                            card?.let { card ->
                                Content(
                                    content = card.content,
                                    cardId = card.id,
                                )
                            }
                        }
                    }

                    is GroupContentModel.Script -> {
                        var scriptResult by remember { mutableStateOf<ScriptResult?>(null) }
                        var isRunningScript by remember { mutableStateOf(false) }
                        var scriptResultKey by remember { mutableStateOf(0) }

                        val runScript: suspend (String, String?, Map<String, String?>, Boolean?) -> Unit =
                            { id, data, input, useCache ->
                                isRunningScript = true
                                try {
                                    api.runScript(id, RunScriptBody(data = data, input = input, useCache = useCache)) {
                                        scriptResult = it
                                        scriptResultKey++
                                    }
                                } finally {
                                    isRunningScript = false
                                }
                            }

                        LaunchedEffect(content.scriptId, content.data) {
                            content.scriptId?.let { scriptId ->
                                api.script(scriptId) {
                                    setTitle(it.name)
                                }
                                runScript(scriptId, content.data, emptyMap(), true)
                            }
                        }

                        scriptResult?.content?.let {
                            Div({
                                classes(Styles.cardContent)
                            }) {
                                StoryContents(
                                    content = it,
                                    key = scriptResultKey,
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

                    is GroupContentModel.Website -> {
                        var url by remember { mutableStateOf(content.url ?: "") }
                        FlexInput(
                            value = url,
                            onChange = {
                                url = it
                            },
                            onSubmit = {
                                api.updateGroup(
                                    group.group!!.id!!,
                                    Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Website(url)))
                                ) {
                                    onUpdated(group.apply { this.group!!.content = it.content })
                                }
                                true
                            },
                            placeholder = "example.com"
                        )
                        content.url?.takeIf { it.isNotBlank() }?.let { iframeUrl ->
                            Iframe({
                                style {
                                    flex(1)
                                    property("border", "none")
                                    marginTop(1.r)
                                    borderRadius(1.r)
                                }
                                attr("src", iframeUrl.ensureScheme())
                                attr("allowfullscreen", "true")
                                attr("allow", "autoplay; fullscreen")
                                ref {
                                    val interval = window.setInterval({
                                        try {
                                            it.contentWindow?.document?.title?.takeIf { it.isNotBlank() }?.let {
                                                setTitle(it)
                                            }
                                        } catch (_: Throwable) {
                                        }
                                    }, 1000)
                                    onDispose {
                                        window.clearInterval(interval)
                                    }
                                }
                            })
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

private fun String.ensureScheme() = if (contains("://")) this else "https://$this"
