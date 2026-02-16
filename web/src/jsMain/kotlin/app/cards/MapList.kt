package app.cards

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.ailaai.api.groupCards
import app.ailaai.api.updateCard
import app.dialog.inputSelectDialog
import application
import com.queatz.db.Card
import com.queatz.db.Person
import com.queatz.db.Task
import components.CardListItem
import components.LazyColumn
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import tagColor
import web.cssom.TextDecoration

@Composable
fun MapList(
    cards: List<Card>,
    allCards: List<Card>? = null,
    showPhoto: Boolean = true,
    people: List<Person>? = null,
    groupId: String? = null,
    onUpdated: (() -> Unit)? = null,
    styles: (StyleScope.() -> Unit)? = null,
    onSelected: (Card) -> Unit
) {
    val scope = rememberCoroutineScope()
    LazyColumn({
        style {
            gap(.5.r)
            alignItems(AlignItems.Stretch)
            styles?.invoke(this)
        }
    }) {
        items(cards) { card ->
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(.5.r)
                    card.task?.done?.let { done ->
                        if (done) {
                            opacity(.5)
                            textDecoration(TextDecoration.lineThrough.toString())
                        }
                    }
                }
            }) {
                Div({
                    style {
                        flex(1)
                        width(0.px)
                    }
                }) {
                    CardListItem(card, showPhoto = showPhoto, people = people) {
                        onSelected(card)
                    }
                }

                card.task?.status?.takeIf { it.isNotBlank() }?.let { status ->
                    Span({
                        classes(Styles.button, Styles.buttonSmall)
                        style {
                            backgroundColor(tagColor(status))
                            color(Color.white)
                            whiteSpace("nowrap")
                            marginRight(.5.r)
                            flexShrink(0)
                        }
                        onClick {
                            it.stopPropagation()
                            if (onUpdated != null) {
                                val items = (allCards ?: cards).mapNotNull { it.task?.status }.filter { it.isNotBlank() }.distinct().sorted()
                                scope.launch {
                                    val newStatus = inputSelectDialog(
                                        confirmButton = application.appString { okay },
                                        placeholder = application.appString { Strings.status },
                                        items = items
                                    )
                                    if (newStatus != null && newStatus != status) {
                                        val updatedCard = card.apply {
                                            task = (task ?: Task()).apply {
                                                this.status = newStatus
                                            }
                                        }
                                        api.updateCard(card.id!!, updatedCard) {
                                            onUpdated()
                                        }
                                    }
                                }
                            }
                        }
                    }) {
                        Text(status)
                    }
                }
            }
        }
    }
}
