package components

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.newCard
import app.ailaai.api.updateCard
import app.components.FlexInput
import app.dialog.inputSelectDialog
import application
import bulletedString
import com.queatz.db.Card
import com.queatz.db.Person
import com.queatz.db.Task
import kotlinx.coroutines.launch
import notBlank
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
    onBackground: Boolean = false,
    isSubtask: Boolean = false,
    expanded: Boolean = false,
    onExpanded: ((Boolean) -> Unit)? = null,
    onUpdated: (() -> Unit)? = null,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    onClick: (Card) -> Unit
) {
    val scope = rememberCoroutineScope()
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Stretch)
        }
        attrs?.invoke(this)
    }) {
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
            val subtaskCount = remember(allCards, card.id, card.task?.owner) {
                allCards?.count { it.task?.owner == card.id } ?: 0
            }
            Div({
                style {
                    flex(1)
                    width(0.px)
                }
            }) {
                CardListItem(
                    card = card,
                    description = remember(allCards, card.id, card.task?.owner) {
                        bulletedString(
                            if (isSubtask) { null } else {
                                card.task?.owner?.let { owner ->
                                    allCards?.find {
                                        it.id == owner
                                    }?.name?.notBlank?.let { "⤷ $it" }
                                }
                            },
                            subtaskCount.takeIf { it > 0 }?.toString()
                        ).notBlank
                    },
                    showPhoto = showPhoto,
                    people = people,
                    isOnSurface = isOnSurface,
                    onBackground = onBackground,
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

            if (onExpanded != null) {
                IconButton(
                    name = if (expanded) "expand_less" else "expand_more",
                    title = application.appString { subtasks },
                    styles = {
                        if (subtaskCount == 0) {
                            opacity(.25f)
                        }
                    }
                ) {
                    onExpanded(!expanded)
                }
            }
        }

        if (expanded) {
            val subtasks = remember(allCards, card.id) {
                allCards?.filter { it.task?.owner == card.id } ?: emptyList()
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    paddingLeft(1.r)
                    gap(.5.r)
                    marginTop(.5.r)
                }
            }) {
                subtasks.forEach { subtask ->
                    TaskListItem(
                        card = subtask,
                        allCards = allCards,
                        showPhoto = showPhoto,
                        people = people,
                        isOnSurface = isOnSurface,
                        onBackground = onBackground,
                        onUpdated = onUpdated,
                        onClick = onClick,
                        isSubtask = true
                    )
                }

                var newSubtaskName by remember { mutableStateOf("") }
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(.5.r)
                    }
                }) {
                    val onSubmit: () -> Boolean = {
                        scope.launch {
                            api.newCard(
                                Card(
                                    group = card.group,
                                    name = newSubtaskName,
                                    task = Task(owner = card.id)
                                )
                            ) {
                                newSubtaskName = ""
                                onUpdated?.invoke()
                            }
                        }

                        true
                    }

                    Div({ style { flex(1) } }) {
                        FlexInput(
                            value = newSubtaskName,
                            onChange = { newSubtaskName = it },
                            placeholder = application.appString { newTask },
                            onDismissRequest = {
                                newSubtaskName = ""
                                onExpanded?.invoke(false)
                            },
                            onSubmit = onSubmit,
                            autoFocus = true,
                        )
                    }
                    if (newSubtaskName.isNotBlank()) {
                        IconButton("send", application.appString { create }) {
                            onSubmit()
                        }
                    }
                }
            }
        }
    }
}
