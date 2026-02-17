package app.dialog

import Styles
import Strings
import androidx.compose.runtime.*
import app.ailaai.api.updateCard
import api
import application
import com.queatz.db.Card
import com.queatz.db.Task
import com.queatz.db.Person
import components.Loading
import components.TaskListItem
import app.compose.rememberDarkMode
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import app.group.friendsDialog

suspend fun batchTasksDialog(
    groupId: String,
    initialCards: List<Card>,
    allCards: List<Card>? = null,
    people: List<Person>? = null,
    onUpdated: () -> Unit
) {
    dialog(
        title = application.appString { batch },
        confirmButton = null,
        cancelButton = application.appString { close }
    ) { resolve ->
        var cards by remember { mutableStateOf(initialCards) }
        var isLoading by remember { mutableStateOf(false) }
        val isDarkMode = rememberDarkMode()
        val scope = rememberCoroutineScope()

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    width(32.r)
                    maxWidth(100.percent)
                    position(Position.Relative)
                    if (isLoading) {
                        property("pointer-events", "none")
                    }
                }
            }) {
                if (isLoading) {
                    Div({
                        style {
                            position(Position.Absolute)
                            left(0.px)
                            top(0.px)
                            right(0.px)
                            bottom(0.px)
                            backgroundColor(if (isDarkMode) rgba(0, 0, 0, 0.5) else rgba(255, 255, 255, 0.5))
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            borderRadius(1.r)
                            property("z-index", 1000)
                            boxSizing("border-box")
                        }
                    }) {
                        Loading()
                    }
                }

                // Action buttons
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexWrap(FlexWrap.Wrap)
                        gap(.5.r)
                    }
                }) {
                    // Set status
                    Div({
                        classes(Styles.outlineButton, Styles.outlineButtonSmall)
                        if (isLoading) {
                            style {
                                opacity(0.5)
                            }
                        }
                        onClick {
                            if (isLoading) return@onClick
                            scope.launch {
                                val items = allCards?.mapNotNull { it.task?.status }?.filter { it.isNotBlank() }?.distinct()?.sorted() ?: emptyList()
                                val newStatus = inputSelectDialog(
                                    confirmButton = application.appString { okay },
                                    placeholder = application.appString { Strings.status },
                                    items = items
                                )
                                if (newStatus != null) {
                                    isLoading = true
                                    cards.forEach { card ->
                                        val updatedCard = card.apply {
                                            task = (task ?: Task()).apply {
                                                status = newStatus
                                            }
                                        }
                                        api.updateCard(card.id!!, updatedCard)
                                    }
                                    isLoading = false
                                    onUpdated()
                                }
                            }
                        }
                    }) {
                        Text(application.appString { setStatus })
                    }

                    // Add category
                    Div({
                        classes(Styles.outlineButton, Styles.outlineButtonSmall)
                        if (isLoading) {
                            style {
                                opacity(0.5)
                            }
                        }
                        onClick {
                            if (isLoading) return@onClick
                            scope.launch {
                                val items = allCards?.flatMap { it.categories.orEmpty() }?.filter { it.isNotBlank() }?.distinct()?.sorted() ?: emptyList()
                                val newCategory = inputSelectDialog(
                                    confirmButton = application.appString { okay },
                                    placeholder = application.appString { Strings.category },
                                    items = items
                                )
                                if (newCategory != null) {
                                    isLoading = true
                                    cards.forEach { card ->
                                        if (newCategory !in (card.categories ?: emptyList())) {
                                            val updatedCard = card.apply {
                                                categories = (categories ?: emptyList()) + newCategory
                                            }
                                            api.updateCard(card.id!!, updatedCard)
                                        }
                                    }
                                    isLoading = false
                                    onUpdated()
                                }
                            }
                        }
                    }) {
                        Text(application.appString { Strings.addCategory })
                    }

                    // Set collaborators
                    Div({
                        classes(Styles.outlineButton, Styles.outlineButtonSmall)
                        if (isLoading) {
                            style {
                                opacity(0.5)
                            }
                        }
                        onClick {
                            if (isLoading) return@onClick
                            scope.launch {
                                 friendsDialog(
                                    title = application.appString { Strings.collaborators },
                                    confirmButton = application.appString { confirm },
                                    multiple = true
                                ) { selected ->
                                    scope.launch {
                                        isLoading = true
                                        val collaborators = selected.mapNotNull { it.id }
                                        cards.forEach { card ->
                                            val ownerId = card.person
                                            val finalCollaborators = collaborators.filter { it != ownerId }
                                            val updatedCard = card.apply {
                                                this.collaborators = finalCollaborators
                                            }
                                            api.updateCard(card.id!!, updatedCard)
                                        }
                                        isLoading = false
                                        onUpdated()
                                    }
                                }
                            }
                        }
                    }) {
                        Text(application.appString { setCollaborators })
                    }

                    // Mark as done
                    Div({
                        classes(Styles.outlineButton, Styles.outlineButtonSmall)
                        if (isLoading) {
                            style {
                                opacity(0.5)
                            }
                        }
                        onClick {
                            if (isLoading) return@onClick
                            scope.launch {
                                isLoading = true
                                cards.forEach { card ->
                                    val updatedCard = card.apply {
                                        task = (task ?: Task()).apply {
                                            done = true
                                        }
                                    }
                                    api.updateCard(card.id!!, updatedCard)
                                }
                                isLoading = false
                                onUpdated()
                            }
                        }
                    }) {
                        Text(application.appString { markAsDone })
                    }

                    // Mark as not done
                    Div({
                        classes(Styles.outlineButton, Styles.outlineButtonSmall)
                        if (isLoading) {
                            style {
                                opacity(0.5)
                            }
                        }
                        onClick {
                            if (isLoading) return@onClick
                            scope.launch {
                                isLoading = true
                                cards.forEach { card ->
                                    val updatedCard = card.apply {
                                        task = (task ?: Task()).apply {
                                            done = false
                                        }
                                    }
                                    api.updateCard(card.id!!, updatedCard)
                                }
                                isLoading = false
                                onUpdated()
                            }
                        }
                    }) {
                        Text(application.appString { markAsNotDone })
                    }
                }

            // List of cards
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(.5.r)
                    overflowY("auto")
                    maxHeight(20.r)
                }
            }) {
                cards.forEach { card ->
                    TaskListItem(
                        card = card,
                        people = people,
                        attrs = {
                            title(application.appString { tapToRemoveFromBatch })
                        }
                    ) { clickedCard ->
                        if (isLoading) return@TaskListItem
                        cards = cards - clickedCard
                        if (cards.isEmpty()) {
                            resolve(true)
                        }
                    }
                }
            }
        }
    }
}
