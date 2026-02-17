package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.ailaai.api.updateCard
import app.dialog.inputSelectDialog
import application
import com.queatz.db.Card
import com.queatz.db.Person
import com.queatz.db.Task
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import r
import tagColor
import web.cssom.TextDecoration

@Composable
fun TaskListItem(
    card: Card,
    allCards: List<Card>? = null,
    showPhoto: Boolean = false,
    people: List<Person>? = null,
    isOnSurface: Boolean = false,
    onUpdated: (() -> Unit)? = null,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    onClick: (Card) -> Unit
) {
    val scope = rememberCoroutineScope()
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
        attrs?.invoke(this)
    }) {
        Div({
            style {
                flex(1)
                width(0.px)
            }
        }) {
            CardListItem(
                card = card,
                showPhoto = showPhoto,
                people = people,
                isOnSurface = isOnSurface,
            ) {
                onClick(card)
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
                        val items = (allCards ?: listOf(card)).mapNotNull { it.task?.status }.filter { it.isNotBlank() }.distinct().sorted()
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
