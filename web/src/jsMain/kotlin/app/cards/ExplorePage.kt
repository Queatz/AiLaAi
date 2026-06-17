package app.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.PageTopBar
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.newCard
import app.ailaai.api.updateCard
import app.components.FlexInput
import app.dialog.editFormDialog
import app.dialog.inputDialog
import app.dialog.rememberChoosePhotoDialog
import appString
import application
import com.queatz.db.Card
import com.queatz.db.StoryContent
import com.queatz.db.Widget
import com.queatz.db.isPart
import com.queatz.db.toJsonStoryPart
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.FormData
import components.CardActivity
import components.CardItem
import components.CardPhotoOrVideo
import components.Content
import components.ContentActions
import components.IconButton
import components.Loading
import components.Switch
import components.getConversation
import hint
import json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import kotlinx.browser.window
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import stories.asStoryContents
import updateWidget
import widget

fun serializeStoryContent(content: List<StoryContent>): String {
    return json.encodeToString(buildJsonArray {
        content.filter { it.isPart() }.forEach { part ->
            add(part.toJsonStoryPart(json))
        }
    })
}

@Composable
fun ExplorePage(
    card: Card,
    onCard: (Card) -> Unit,
    onCardUpdated: (Card) -> Unit,
    onCardDeleted: (card: Card) -> Unit,
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    val choosePhotoDialog = rememberChoosePhotoDialog(showUpload = true)
    var cards by remember(card.id) {
        mutableStateOf(listOf<Card>())
    }

    var isLoading by remember(card.id) {
        mutableStateOf(true)
    }

    var menuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }
    var formReloadKey by remember { mutableStateOf(0) }

    suspend fun reload() {
        if (me == null) return
        api.cardsCards(card.id!!) {
            cards = it
        }
        isLoading = false
    }

    LaunchedEffect(card.id) {
        reload()
    }

    fun newSubCard(inCard: Card, name: String, active: Boolean) {
        scope.launch {
            api.newCard(Card(name = name, parent = inCard.id!!, active = active)) {
                reload()
                onCardUpdated(it)
            }
        }
    }

    val titleString = appString { title }
    val rename = appString { rename }
    val hint = appString { hint }
    val update = appString { update }

    fun rename() {
        scope.launch {
            val name = inputDialog(
                title = titleString,
                placeholder = "",
                confirmButton = rename,
                defaultValue = card.name ?: ""
            )

            if (name == null) {
                return@launch
            }

            api.updateCard(card.id!!, Card(name = name)) {
                onCardUpdated(it)
            }
        }
    }

    fun rehint() {
        scope.launch {
            val hint = inputDialog(
                hint,
                "",
                update,
                defaultValue = card.location ?: ""
            )

            if (hint == null) {
                return@launch
            }

            api.updateCard(card.id!!, Card(location = hint)) {
                onCardUpdated(it)
            }
        }
    }

    if (menuTarget != null) {
        ExplorePageMenu(
            card = card,
            menuTarget = menuTarget!!,
            choosePhotoDialog = choosePhotoDialog,
            onCard = onCard,
            onCardUpdated = onCardUpdated,
            onCardDeleted = onCardDeleted,
            onDismiss = { menuTarget = null },
            scope = scope
        )
    }

    var published by remember(card) {
        mutableStateOf(card.active == true)
    }

    PageTopBar(
        title = card.name?.notBlank ?: appString { newCard },
        description = card.hint,
        onTitleClick = if (me?.id == card.person) {
            {
                rename()
            }
        } else null,
        onDescriptionClick = if (me?.id == card.person) {
            {
                rehint()
            }
        } else null,
        navActions = {
            if (card.parent != null) {
                IconButton("arrow_upward", appString { openEnclosingCard }, styles = {
                    marginRight(.5f.r)
                }) {
                    scope.launch {
                        api.card(card.parent!!) {
                            onCard(it)
                        }
                    }
                }
            }

        },
        isMenuLoading = choosePhotoDialog.isGenerating.collectAsState().value,
        actions = {
            Switch(
                value = published,
                onValue = { published = it },
                onChange = {
                    scope.launch {
                        val previousValue = card.active == true
                        api.updateCard(card.id!!, Card(active = it), onError = {
                            published = previousValue
                        }) {
                            onCardUpdated(it)
                        }
                    }
                },
                title = if (published) {
                    appString { pageIsPublished }
                } else {
                    appString { pageIsNotPublished }
                }
            ) {
                margin(1.r)
            }
        }
    ) {
        menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
    }

    val conversation = remember(card) {
        card.getConversation()
    }

    suspend fun saveConversation(value: String): Boolean {
        conversation.message = value
        val conversationString = json.encodeToString(conversation)

        var success = false

        api.updateCard(card.id!!, Card(conversation = conversationString)) {
            success = true
            onCardUpdated(card)
        }

        return success
    }

    Div({
        style {
            flex(1)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            overflowY("auto")
            overflowX("hidden")
        }
    }) {
        if (!card.photo.isNullOrBlank() || !card.video.isNullOrBlank()) {
            CardPhotoOrVideo(
                card = card,
                defaultWidth = false
            ) {
                borderRadius(1.r)
                margin(0.r, 1.r)
            }
        }
        Div({
            style {
                margin(1.r, 1.r, .5.r, 1.r)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
            }
        }) {
            card.activity?.let {
                CardActivity(it, card)
            }
            Content(
                content = card.content,
                cardId = card.id,
                onCardClick = { cardId, openInNewWindow ->
                    if (openInNewWindow) {
                        window.open("/page/$cardId", target = "_blank")
                    } else {
                        scope.launch {
                            api.card(cardId) { clickedCard ->
                                onCard(clickedCard)
                            }
                        }
                    }
                },
                editable = me?.id == card.person,
                onEdited = { index, part ->
                    scope.launch {
                        val current = card.content?.asStoryContents() ?: emptyList()
                        val newContent = current.toMutableList()
                        newContent[index] = part
                        api.updateCard(
                            card.id!!,
                            Card(content = serializeStoryContent(newContent))
                        ) { onCardUpdated(it) }
                    }
                },
                onReorder = { fromIndex, toIndex ->
                    scope.launch {
                        val current = card.content?.asStoryContents() ?: emptyList()
                        val newContent = current.toMutableList()
                        val item = newContent.removeAt(fromIndex)
                        newContent.add(if (fromIndex < toIndex) toIndex - 1 else toIndex, item)
                        api.updateCard(
                            id = card.id!!,
                            card = Card(content = serializeStoryContent(newContent))
                        ) { onCardUpdated(it) }
                    }
                },
                onSave = { newContent ->
                    scope.launch {
                        api.updateCard(
                            card.id!!,
                            Card(content = serializeStoryContent(newContent))
                        ) { onCardUpdated(it) }
                    }
                },
                actions = @Composable { index, part ->
                    val current = card.content?.asStoryContents() ?: emptyList()
                    ContentActions(
                        index = index,
                        part = part,
                        isEditable = me?.id == card.person,
                        currentContent = current,
                        onContentUpdated = { newContent ->
                            scope.launch {
                                api.updateCard(
                                    id = card.id!!,
                                    card = Card(content = serializeStoryContent(newContent))
                                ) { onCardUpdated(it) }
                            }
                        },
                        additionalActions = {
                            // Form widget specific actions
                            if (part is StoryContent.Widget && part.widget == Widgets.Form) {
                                IconButton("edit", appString { edit }) {
                                    scope.launch {
                                        api.widget(part.id) { w ->
                                            val dataJson = w.data ?: return@widget
                                            val initialForm = try {
                                                json.decodeFromString<FormData>(dataJson)
                                            } catch (e: Exception) {
                                                return@widget
                                            }
                                            editFormDialog(
                                                initialFormData = initialForm,
                                                isEdit = true
                                            ) { updatedForm ->
                                                api.updateWidget(
                                                    id = part.id,
                                                    widget = Widget(data = json.encodeToString(updatedForm))
                                                ) {
                                                    formReloadKey++
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                },
                formReloadKey = formReloadKey
            )
        }

        // Todo full conversation support

//            if (conversation.message.isNotBlank()) {
//                Div({
//                    style {
//                        padding(1.r)
//                        whiteSpace("pre-wrap")
//                    }
//                }) {
//                    Text(conversation.message)
//                }
//            }

        var messageText by remember(conversation.message) { mutableStateOf(conversation.message) }
        var messageChanged by remember(conversation.message) { mutableStateOf(false) }
        var isSaving by remember(conversation.message) { mutableStateOf(false) }

        Div({
            style {
                margin(.5.r, 1.r)
                maxHeight(50.vh)
            }
        }) {
            FlexInput(
                value = messageText,
                onChange = { newText ->
                    messageText = newText
                    messageChanged = true
                },
                initialValue = conversation.message,
                placeholder = appString { details },
                showButtons = true,
                buttonText = appString { save },
                onSubmit = {
                    isSaving = true
                    val result = saveConversation(messageText)
                    if (result) {
                        messageChanged = false
                    }
                    isSaving = false

                    result
                }
            )
        }

        NewCardInput(
            styles = {
                flexShrink(0)
            },
            onSubmit = { name, active ->
                newSubCard(card, name, active)
            }
        )

        if (isLoading) {
            Loading()
        } else {
            Div(
                {
                    classes(CardsPageStyles.layout)
                    style {
                        paddingBottom(1.r)
                    }
                }
            ) {
                cards.forEach { card ->
                    CardItem(card, showTapToOpen = false) {
                        onCard(card)
                    }
                }
            }
        }
    }
}
