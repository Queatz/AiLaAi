package app.group

import LocalConfiguration
import Strings.card
import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.card
import app.ailaai.api.groupCards
import app.ailaai.api.runScript
import app.ailaai.api.updateGroup
import app.components.Empty
import app.components.FlexInput
import app.dialog.selectCardDialog
import app.dialog.selectScriptDialog
import app.nav.CardItem
import components.CardItem as CardItemById
import app.scripts.ResultPanel
import application
import json
import com.queatz.db.Card
import com.queatz.db.GroupContent as GroupContentModel
import com.queatz.db.GroupExtended
import com.queatz.db.RunScriptBody
import com.queatz.db.ScriptResult
import components.Content
import components.Loading
import components.Markdown
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r
import saves
import stories.StoryContents

@Composable
fun GroupContent(group: GroupExtended, onUpdated: (GroupExtended) -> Unit) {
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
                icon = "text_fields",
                title = "Text",
                description = "Add text content"
            ) {
                scope.launch {
                    api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Text("")))) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
            FeatureButton(
                icon = "check_box",
                title = "Tasks",
                description = "Add a list of tasks"
            ) {
                scope.launch {
                    api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Tasks()))) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
            FeatureButton(
                icon = "description",
                title = "Card",
                description = "Link to a page"
            ) {
                scope.launch {
                    selectCardDialog(configuration) { card ->
                        scope.launch {
                            api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Card(card.id)))) {
                                onUpdated(group.apply { this.group!!.content = it.content })
                            }
                        }
                    }
                }
            }
            FeatureButton(
                icon = "code",
                title = "Script",
                description = "Run a script"
            ) {
                scope.launch {
                    selectScriptDialog(scope) { scriptId, scriptData ->
                        scope.launch {
                            api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Script(scriptId, scriptData)))) {
                                onUpdated(group.apply { this.group!!.content = it.content })
                            }
                        }
                    }
                }
            }
            FeatureButton(
                icon = "public",
                title = "Website",
                description = "Link to a website"
            ) {
                scope.launch {
                    api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Website("")))) {
                        onUpdated(group.apply { this.group!!.content = it.content })
                    }
                }
            }
        }
    } else {
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
                                api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Text(text)))) {
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
                    LaunchedEffect(group.group!!.id) {
                        api.groupCards(group.group!!.id!!) {
                            cards = it
                        }
                    }

                    if (cards == null) {
                        Loading()
                    } else if (cards!!.isEmpty()) {
                        Empty { Text("No tasks.") }
                    } else {
                        cards!!.forEach { card ->
                            CardItem(
                                card = card,
                                scroll = false,
                                selected = false,
                                saved = saves.cards.value.any { it.id == card.id },
                                published = card.active == true
                            ) {
                                // todo
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

                    val runScript: suspend (String, String?, Map<String, String?>, Boolean?) -> Unit = { id, data, input, useCache ->
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
                        content.scriptId?.let {
                            runScript(it, content.data, emptyMap(), true)
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
                            api.updateGroup(group.group!!.id!!, com.queatz.db.Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Website(url)))) {
                                onUpdated(group.apply { this.group!!.content = it.content })
                            }
                            true
                        },
                        placeholder = "https://example.com"
                    )
                    content.url?.takeIf { it.isNotBlank() }?.let { iframeUrl ->
                        Iframe({
                            style {
                                flex(1)
                                property("border", "none")
                                marginTop(1.r)
                                borderRadius(1.r)
                            }
                            attr("src", iframeUrl)
                            attr("allowfullscreen", "true")
                            attr("allow", "autoplay; fullscreen")
                        })
                    }
                }
                else -> Unit
            }
        }
    }
}
