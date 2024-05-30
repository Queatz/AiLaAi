package app.widget

import Styles
import Styles.card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppNavigation
import app.AppStyles
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.newCard
import app.appNav
import app.cards.NewCardInput
import app.components.Empty
import app.dialog.inputDialog
import app.nav.NavSearchInput
import app.softwork.routingcompose.Router
import appString
import application
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.PageTreeData
import components.Icon
import components.getConversation
import focusable
import isMine
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import lib.toLocaleString
import notBlank
import notEmpty
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.outline
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import updateWidget
import widget
import kotlin.random.Random
import kotlin.random.Random.Default.nextInt

@Composable
fun PageTreeWidget(widgetId: String) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    val router = Router.current
    val appNav = appNav
    var widget by remember(widgetId) {
        mutableStateOf<Widget?>(null)
    }
    var isMine by remember(widgetId) {
        mutableStateOf(false)
    }
    var cards by remember(widgetId) {
        mutableStateOf<List<Card>>(emptyList())
    }
    var search by remember(widgetId) {
        mutableStateOf("")
    }
    var tagFilter by remember(widgetId) {
        mutableStateOf("")
    }
    var data by remember(widgetId) {
        mutableStateOf<PageTreeData?>(null)
    }
    val shownCards = remember(cards, search, tagFilter) {
        if (search.isNotBlank()) {
            cards.filter {
                it.name?.contains(search, ignoreCase = true) == true
            }
        } else {
            cards
        }.let {
            if (tagFilter.isNotBlank()) {
                it.filter {
                    data?.tags?.get(it.id!!)?.contains(tagFilter) == true
                }
            } else {
                it
            }
        }
    }
    val allTags = remember(data) {
        data?.tags?.values?.flatten()?.distinct()?.sorted()
    }

    suspend fun reload() {
        api.cardsCards(data?.card ?: return) {
            cards = it
        }
    }

    fun newSubCard(inCardId: String, name: String, active: Boolean) {
        scope.launch {
            api.newCard(Card(name = name, parent = inCardId, active = active)) {
                reload()
            }
        }
    }

    LaunchedEffect(widgetId, me, data) {
        api.card(data?.card ?: return@LaunchedEffect) {
            isMine = it.isMine(me?.id)
        }
    }

    LaunchedEffect(widgetId) {
        // todo loading
        api.widget(widgetId) {
            it.data ?: return@widget

            widget = it
            data = json.decodeFromString<PageTreeData>(it.data!!)

            reload()
        }
    }

    suspend fun save(widgetData: PageTreeData) {
        api.updateWidget(widgetId, Widget(data = json.encodeToString(widgetData))) {
            widget = it
            data = json.decodeFromString<PageTreeData>(it.data!!)
        }
    }

    fun tagColor(tag: String): CSSColorValue {
        val hue = Random(tag.hashCode()).nextInt(360)
        return Color("hsl($hue, 60%, 40%)")
    }

    fun removeTag(card: Card, tag: String) {
        scope.launch {
            api.updateWidget(widgetId, Widget(data = json.encodeToString(data!!.copy(tags = data!!.tags.toMutableMap().apply {
                put(card.id!!, getOrElse(card.id!!) { emptyList() } - tag)
            })))) {
                widget = it
                data = json.decodeFromString<PageTreeData>(it.data!!)
            }
        }
    }

    fun addTag(card: Card) {
        scope.launch {
            // todo: translate
            val tag = inputDialog(null, confirmButton = "Add tag") { resolve, onValue ->
                Div({
                    style {
                        overflowY("auto")
                        maxHeight(8.r)
                    }
                }) {
                    allTags?.forEach { tag ->
                        Div({
                            classes(
                                listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                            )

                            style {
                                backgroundColor(tagColor(tag))
                                marginTop(.5.r)
                            }

                            onClick {
                                onValue(tag)
                                resolve(true)
                            }

                            focusable()
                        }) {
                            Div {
                                Div({
                                    classes(AppStyles.groupItemName)
                                }) {
                                    Text(tag)
                                }
                            }
                        }
                    }
                }
            }

            if (!tag.isNullOrBlank()) {
                api.updateWidget(widgetId, Widget(data = json.encodeToString(data!!.copy(tags = data!!.tags.toMutableMap().apply {
                    put(card.id!!, (getOrElse(card.id!!) { emptyList() } + tag).distinct())
                })))) {
                    widget = it
                    data = json.decodeFromString<PageTreeData>(it.data!!)
                }
            }
        }
    }

    Div(
        {
            classes(WidgetStyles.pageTree)
        }
    ) {
        if (isMine) {
            NewCardInput(defaultMargins = false) { name, active ->
                newSubCard(data?.card ?: return@NewCardInput, name, active)
            }
        }

        if (cards.size > 5) {
            NavSearchInput(
                search,
                { search = it },
                defaultMargins = false,
                autoFocus = false,
                styles = {
                    width(100.percent)
                    marginBottom(1.r)
                }
            )

            allTags?.notEmpty?.let { tags ->
                Tags(
                    tags = tags,
                    selected = tagFilter,
                    // todo: translate
                    title = "Tap to filter",
                    tagColor = ::tagColor,
                    onClick = {
                        tagFilter = if (tagFilter == it) "" else it
                    }
                )
            }
        }

        if (search.isNotBlank() && shownCards.isEmpty()) {
            Empty {
                Text(appString { noCards })
            }
        }

        shownCards.sortedByDescending {
            data?.votes?.get(it.id!!) ?: 0
        }.forEach { card ->
            key(card.id!!) {
                val votes = data?.votes?.get(card.id!!) ?: 0
                Div({
                    classes(WidgetStyles.pageTreeItem)
                }) {
                    Div({
                        style {
                            textAlign("center")
                            marginRight(1.r)
                        }
                    }) {
                        if (me != null) {
                            Button({
                                classes(Styles.outlineButton)

                                title("+1 vote")

                                onClick {
                                    it.stopPropagation()

                                    scope.launch {
                                        save(
                                            data!!.copy(
                                                votes = data!!.votes.toMutableMap().apply {
                                                    put(card.id!!, (data!!.votes[card.id!!] ?: 0) + 1)
                                                }
                                            )
                                        )
                                    }
                                }
                            }) {
                                // todo: translate
                                Text("Vote")
                            }
                        }

                        if (votes != 0 || me == null) {
                            Div({
                                style {
                                    if (me != null) {
                                        cursor("pointer")
                                        marginTop(.5.r)
                                    }
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    alignItems(AlignItems.Center)
                                }

                                if (me != null) {
                                    // todo: translate
                                    title("Edit votes")

                                    onClick {
                                        it.stopPropagation()

                                        scope.launch {
                                            val result = inputDialog(
                                                // todo: translate
                                                "Votes",
                                                confirmButton = application.appString { update },
                                                defaultValue = data!!.votes[card.id!!]?.toString() ?: "0"
                                            )

                                            result ?: return@launch

                                            save(
                                                data!!.copy(
                                                    votes = data!!.votes.toMutableMap().apply {
                                                        put(card.id!!, result.toIntOrNull() ?: 0)
                                                    }
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    // todo: translate
                                    title("Sign in to vote")
                                }
                            }) {
                                // todo: translate
                                if (me != null) {
                                    Text("${votes.toLocaleString()} ${if (votes == 1) "vote" else "votes"}")
                                } else {
                                    Div({
                                        style {
                                            fontSize(24.px)
                                            fontWeight("bold")
                                        }
                                    }) {
                                        Text(votes.toLocaleString())
                                    }
                                    Text(if (votes == 1) "vote" else "votes")
                                }
                            }
                        }
                    }
                    Div({
                        style {
                            cursor("pointer")
                            flexGrow(1)
                        }

                        // todo: translate
                        title("Open page")

                        onClick { event ->
                            event.stopPropagation()

                            if (event.ctrlKey) {
                                window.open("/page/${card.id!!}", target = "_blank")
                            } else {
                                if (card.person == me?.id) {
                                    scope.launch {
                                        appNav.navigate(AppNavigation.Page(card.id!!, card))
                                    }
                                } else {
                                    router.navigate("/page/${card.id!!}")
                                }
                            }
                        }
                    }) {
                        Div({
                            style {
                                fontWeight("bold")
                                fontSize(18.px)
                            }
                        }) {
                            Text(card.name ?: "")
                        }

                        card.getConversation().message.notBlank?.let {
                            Div({
                                style {
                                    fontSize(16.px)
                                }
                            }) {
                                Text(it)
                            }
                        }

                        val tags = data?.tags?.get(card.id!!) ?: emptyList()

                        Tags(
                            tags = tags,
                            // todo: translate
                            title = "Tap to remove",
                            tagColor = ::tagColor,
                            onClick = {
                                removeTag(card, it)
                            }
                        ) {
                            Button(
                                {
                                    classes(Styles.outlineButton)

                                    style {
                                        padding(0.r, 1.5.r)
                                        height(2.5.r)
                                    }

                                    // todo: translate
                                    title("Add tag")

                                    onClick {
                                        it.stopPropagation()
                                        addTag(card)
                                    }
                                }
                            ) {
                                Icon("new_label") {
                                    marginRight(0.r)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Tags(
    tags: List<String>,
    selected: String? = null,
    tagColor: (String) -> CSSColorValue,
    title: String,
    onClick: (tag: String) -> Unit,
    content: @Composable () -> Unit = {}
) {
    Div({
        style {
            marginTop(1.r)
            display(DisplayStyle.Flex)
            flexWrap(FlexWrap.Wrap)
            gap(.5.r)
        }
    }) {
        tags.forEach { tag ->
            Button(
                {
                    classes(Styles.button)

                    if (selected == tag) {
                        classes(Styles.buttonSelected)
                    }

                    style {
                        height(2.5.r)
                        color(Color.white)
                        padding(0.r, 1.5.r)
                        backgroundColor(tagColor(tag))
                    }

                    title(title)

                    onClick {
                        it.stopPropagation()
                        onClick(tag)
                    }
                }
            ) {
                Text(tag)
            }
        }

        content()
    }
}
