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
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.newCard
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
import components.getConversation
import isMine
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import lib.toLocaleString
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import r
import updateWidget
import widget

@Composable
fun PageTreeWidget(widgetId: String) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    val router = Router.current
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
    val shownCards = remember(cards, search) {
        if (search.isNotBlank()) {
            cards.filter {
                it.name?.contains(search, ignoreCase = true) == true
            }
        } else {
            cards
        }
    }
    var data by remember(widgetId) {
        mutableStateOf<PageTreeData?>(null)
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

    Div(
        {
            classes(WidgetStyles.pageTree)
        }
    ) {
        if (cards.size > 5) {
            NavSearchInput(
                search,
                { search = it },
                defaultMargins = false,
                styles = {
                    width(100.percent)
                    marginBottom(1.r)
                }
            )
        }

        if (isMine) {
            NewCardInput(defaultMargins = false) { name, active ->
                newSubCard(data?.card ?: return@NewCardInput, name, active)
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
                                    cursor("pointer")
                                    if (me != null) {
                                        marginTop(.5.r)
                                    }
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
                                                        put(card.id!!, result?.toIntOrNull() ?: 0)
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                            }) {
                                // todo: translate
                                Text("${votes.toLocaleString()} ${if (votes == 1) "vote" else "votes"}")
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
                                router.navigate("/page/${card.id!!}")
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

                        Div({
                            style {
                                fontSize(16.px)
                            }
                        }) {
                            Text(card.getConversation().message)
                        }
                    }
                }
            }
        }
    }
}
