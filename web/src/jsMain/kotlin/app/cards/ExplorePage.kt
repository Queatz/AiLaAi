package app.cards

import LocalConfiguration
import Strings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.PageTopBar
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.categories
import app.ailaai.api.deleteCard
import app.ailaai.api.generateCardPhoto
import app.ailaai.api.groups
import app.ailaai.api.myCollaborations
import app.ailaai.api.newCard
import app.ailaai.api.updateCard
import app.components.FlexInput
import app.dialog.categoryDialog
import app.dialog.dialog
import app.dialog.editFormDialog
import app.dialog.inputDialog
import app.dialog.rememberChoosePhotoDialog
import app.dialog.searchDialog
import app.dialog.selectCardDialog
import app.dialog.selectGroupDialog
import app.group.GroupInfo
import app.group.GroupItem
import app.menu.InlineMenu
import app.menu.Menu
import app.messaages.inList
import appString
import appText
import application
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import com.queatz.db.Pay
import com.queatz.db.PayFrequency
import com.queatz.db.StoryContent
import com.queatz.db.Widget
import com.queatz.db.asGeo
import com.queatz.db.isPart
import com.queatz.db.toJsonStoryPart
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.FormData
import components.CardItem
import components.CardPhotoOrVideo
import components.Content
import components.ContentActions
import components.IconButton
import components.Loading
import components.Switch
import components.getConversation
import focusable
import hint
import json
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import qr
import r
import saves
import stories.asStoryContents
import updateWidget
import webBaseUrl
import widget
import kotlin.time.Duration.Companion.seconds

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

    var oldPhoto by remember(card.id) {
        mutableStateOf<String?>(null)
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

    LaunchedEffect(oldPhoto) {
        var tries = 0
        while (tries++ < 5 && oldPhoto != null) {
            delay(3.seconds)
            api.card(card.id!!) {
                if (it.photo != oldPhoto) {
                    onCardUpdated(it)
                    oldPhoto = null
                }
            }
        }
    }

    fun newSubCard(inCard: Card, name: String, active: Boolean) {
        scope.launch {
            api.newCard(Card(name = name, parent = inCard.id!!, active = active)) {
                reload()
                onCardUpdated(it)
            }
        }
    }

    fun generatePhoto() {
        scope.launch {
            api.generateCardPhoto(card.id!!) {
                oldPhoto = card.photo ?: ""
            }
        }
    }

    fun moveToPage(cardId: String) {
        scope.launch {
            api.updateCard(
                card.id!!,
                Card(
                    offline = false,
                    parent = cardId,
                    group = null,
                    equipped = false,
                    geo = null
                )
            ) {
                onCardUpdated(it)
            }
        }
    }

    fun moveToGroup(groupId: String) {
        scope.launch {
            api.updateCard(
                card.id!!,
                Card(
                    offline = false,
                    parent = null,
                    group = groupId,
                    equipped = false,
                    geo = null
                )
            ) {
                onCardUpdated(it)
            }
        }
    }

    val configuration = LocalConfiguration.current
    val inAPage = appString { inAPage }
    val cancel = appString { cancel }
    val titleString = appString { title }
    val rename = appString { rename }
    val category = appString { category }
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

    fun moveToGroup() {
        scope.launch {
            val group = selectGroupDialog(configuration)
            if (group != null) {
                moveToGroup(group.group!!.id!!)
            }
        }
    }

    fun moveToPage() {
        scope.launch {
            selectCardDialog(configuration, title = inAPage) {
                it?.id?.let { id -> moveToPage(id) }
            }
        }
    }

    fun setCategory() {
        scope.launch {
            api.categories(
                // todo: might need to not require geo
                geo = application.me.value?.geo?.asGeo() ?: return@launch
            ) { categories ->
                val category = categoryDialog(
                    categories = categories,
                    category = card.categories?.firstOrNull(),
                )

                if (category != null) {
                    api.updateCard(
                        id = card.id!!,
                        card = Card(categories = category.inList())
                    ) {
                        onCardUpdated(it)
                    }
                }
            }
        }
    }

    if (menuTarget != null) {
        Menu({ menuTarget = null }, menuTarget!!) {
            val isSaved = saves.cards.value.any { it.id == card.id }
            item(appString { openInNewTab }, icon = "open_in_new") {
                window.open("/page/${card.id}", target = "_blank")
            }
            item(if (isSaved) appString { unsave } else appString { save }) {
                scope.launch {
                    if (isSaved) {
                        saves.unsave(card.id!!)
                    } else {
                        saves.save(card.id!!)
                    }
                }
            }

            item(rename) {
                rename()
            }

            item(category) {
                setCategory()
            }

            item(hint) {
                rehint()
            }

            val location = appString { location }
            val close = appString { close }

            item(location) {
                scope.launch {
                    val name = dialog(
                        location,
                        close,
                        null
                    ) {
                        InlineMenu({
                            it(true)
                        }) {
                            item(appString { onProfile }, selected = card.equipped == true, icon = "account_circle") {
                                scope.launch {
                                    api.updateCard(
                                        card.id!!,
                                        Card(offline = false, parent = null, equipped = true, geo = null)
                                    ) {
                                        onCardUpdated(it)
                                    }
                                }
                            }
                            item(
                                appString { inAGroup },
                                selected = card.parent == null && card.offline != true && card.equipped != true && card.group != null,
                                icon = "group"
                            ) {
                                moveToGroup()
                            }
                            item(
                                appString { atALocation },
                                selected = card.parent == null && card.offline != true && card.equipped != true && card.geo != null,
                                icon = "location_on"
                            ) {
                                // todo: needs map
                            }
                            item(inAPage, selected = card.parent != null, icon = "description") {
                                moveToPage()
                            }
                            item(appString { offline }, selected = card.offline == true) {
                                scope.launch {
                                    api.updateCard(
                                        card.id!!,
                                        Card(offline = true, parent = null, equipped = false, geo = null)
                                    ) {
                                        onCardUpdated(it)
                                    }
                                }
                            }
                            item(
                                appString { none },
                                selected = card.parent == null && card.group == null && card.offline != true && card.equipped != true && card.geo == null
                            ) {
                                scope.launch {
                                    api.updateCard(
                                        card.id!!,
                                        Card(offline = false, parent = null, equipped = false, geo = null)
                                    ) {
                                        onCardUpdated(it)
                                    }
                                }
                            }
                        }
                    }

                    if (name == null) {
                        return@launch
                    }
                }
            }

            item(appString { if (card.pay == null) addPay else changePay }) {
                scope.launch {
                    var updatedPay = card.pay ?: Pay(pay = "")

                    val result = dialog(
                        application.appString { pay },
                        confirmButton = application.appString { this.update }
                    ) { resolve ->
                        var pay by remember {
                            mutableStateOf(updatedPay.pay ?: "")
                        }

                        var payFrequency by remember {
                            mutableStateOf(updatedPay.frequency)
                        }

                        LaunchedEffect(pay, payFrequency) {
                            updatedPay.pay = pay
                            updatedPay.frequency = payFrequency
                        }

                        FlexInput(
                            value = pay,
                            onChange = { pay = it },
                            singleLine = true,
                            autoFocus = true,
                            selectAll = true,
                            styles = {
                                width(100.percent)
                                marginBottom(1.r)
                            },
                            onSubmit = {
                                resolve(true)
                                true
                            }
                        )

                        PayFrequency.entries.forEach { frequency ->
                            Div({
                                classes(
                                    listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                                )

                                if (payFrequency == frequency) {
                                    classes(AppStyles.groupItemSelected)
                                }

                                onClick {
                                    payFrequency = if (payFrequency == frequency) null else frequency
                                }

                                focusable()
                            }) {
                                Div {
                                    Div({
                                        classes(AppStyles.groupItemName)
                                    }) {
                                        Text(frequency.appString)
                                    }
                                }
                            }
                        }
                    }

                    if (result == true) {
                        api.updateCard(card.id!!, Card(pay = updatedPay)) {
                            onCardUpdated(it)
                        }
                    }
                }
            }

            item(appString { choosePhoto }) {
                choosePhotoDialog.launch { photo, _, _ ->
                    api.updateCard(card.id!!, Card(photo = photo)) {
                        onCardUpdated(it)
                    }
                }
            }

            if (card.video == null) {
                item(if (card.photo == null) appString { this.generatePhoto } else appString { regeneratePhoto }) {
                    if (card.photo == null) {
                        generatePhoto()
                    } else {
                        scope.launch {
                            val result = dialog(
                                title = application.appString { generateNewPhoto }
                            ) {
                                appText { thisWillReplaceCurrentPhoto }
                            }

                            if (result == true) {
                                generatePhoto()
                            }
                        }
                    }
                }
            }

            if (card.parent != null) {
                item(appString { openEnclosingCard }) {
                    scope.launch {
                        api.card(card.parent!!) {
                            onCard(it)
                        }
                    }
                }
            }

            item(appString { qrCode }) {
                scope.launch {
                    dialog("", cancelButton = null) {
                        val qrCode = remember {
                            "$webBaseUrl/page/${card.id!!}".qr
                        }
                        Img(src = qrCode) {
                            style {
                                borderRadius(1.r)
                            }
                        }
                    }
                }
            }

            item(appString { delete }) {
                scope.launch {
                    val result = dialog(
                        title = application.appString { deleteThisPage },
                        confirmButton = application.appString { yesDelete }
                    ) {
                        appText { youCannotUndoThis }
                    }

                    if (result == true) {
                        api.deleteCard(card.id!!) {
                            onCardDeleted(card)
                        }
                    }
                }
            }
        }
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
