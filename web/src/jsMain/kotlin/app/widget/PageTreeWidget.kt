package app.widget

import PageTreeData
import Styles
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
import app.ailaai.api.cardsCards
import app.dialog.inputDialog
import app.softwork.routingcompose.Router
import application
import com.queatz.db.Card
import com.queatz.db.Widget
import components.getConversation
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
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
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
    var cards by remember(widgetId) {
        mutableStateOf<List<Card>>(emptyList())
    }
    var data by remember(widgetId) {
        mutableStateOf<PageTreeData?>(null)
    }

    LaunchedEffect(widgetId) {
        // todo loading
        api.widget(widgetId) {
            it.data ?: return@widget

            widget = it
            data = json.decodeFromString<PageTreeData>(it.data!!)

            api.cardsCards(data?.card ?: return@widget) {
                cards = it
            }
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
        cards.sortedByDescending {
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
                        if (votes != 0) {
                            Div({
                                style {
                                    cursor("pointer")
                                    marginTop(.5.r)
                                }

                                // todo: translate
                                title("Edit votes")

                                onClick {
                                    it.stopPropagation()

                                    scope.launch {
                                        val result = inputDialog(
                                            "Votes",
                                            confirmButton = application.appString { update },
                                            defaultValue = data!!.votes[card.id!!]?.toString() ?: "0"
                                        )

                                        save(
                                            data!!.copy(
                                                votes = data!!.votes.toMutableMap().apply {
                                                    put(card.id!!, result?.toIntOrNull() ?: 0)
                                                }
                                            )
                                        )
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
