package app.dialog

import Strings.newCard
import Styles
import androidx.compose.runtime.*
import api
import app.AppNavigation
import app.group.friendsDialog
import app.ailaai.api.*
import app.appNav
import app.components.FlexInput
import appString
import application
import com.queatz.db.*
import components.GroupPhoto
import components.GroupPhotoItem
import components.Icon
import components.IconButton
import components.getConversation
import json
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r
import tagColor

suspend fun editTaskDialog(groupId: String, card: Card? = null, allCards: List<Card>? = null, onUpdated: () -> Unit) {
    var nameValue = card?.name ?: ""
    var descriptionValue = card?.getConversation()?.message ?: ""
    var statusValue = card?.task?.status ?: ""
    var doneValue = card?.task?.done ?: false
    var categoriesValue = card?.categories ?: emptyList<String>()
    var collaboratorsValue = card?.collaborators ?: emptyList<String>()

    val isNew = card == null

    val result = dialog(
        title = if (isNew) application.appString { newTask } else null,
        confirmButton = application.appString { save },
        cancelButton = application.appString { cancel },
        extraButtons = {
            val scope = rememberCoroutineScope()

            if (card != null) {
                IconButton("edit", application.appString { edit }, application.appString { edit }) {
                    scope.launch {
                        appNav.navigate(AppNavigation.Page(card.id!!, card))
                    }
                }
            }
        }
    ) { resolve ->
        var name by remember { mutableStateOf(nameValue) }
        var description by remember { mutableStateOf(descriptionValue) }
        var status by remember { mutableStateOf(statusValue) }
        var done by remember { mutableStateOf(doneValue) }
        var categories by remember { mutableStateOf(categoriesValue) }
        var collaborators by remember { mutableStateOf(collaboratorsValue) }
        var people by remember { mutableStateOf(emptyList<Person>()) }

        val scope = rememberCoroutineScope()

        LaunchedEffect(card?.id) {
            card?.id?.let { id ->
                api.cardPeople(id) {
                    // We need to exclude the card owner here
                    people = it.filter { it.id!! in (card.collaborators ?: emptyList()) }
                }
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
                width(32.r)
                maxWidth(100.percent)
            }
        }) {
            // Card name
            FlexInput(
                value = name,
                onChange = { name = it; nameValue = it },
                placeholder = application.appString { Strings.name },
                singleLine = true
            )

            // Card owner + collaborators
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(.5.r)
                    cursor("pointer")
                }
                onClick {
                    scope.launch {
                        friendsDialog(
                            title = application.appString { Strings.collaborators },
                            preselect = collaborators.toSet(),
                            multiple = true
                        ) { selected ->
                            val ownerId = card?.person ?: application.me.value?.id
                            collaborators = selected.mapNotNull { it.id }.filter { it != ownerId }
                            collaboratorsValue = collaborators
                            people = selected.toList()
                        }
                    }
                }
            }) {
                GroupPhoto(
                    items = people.map { GroupPhotoItem(it.photo, it.name) },
                    size = 32.px
                )
                if (people.isNotEmpty()) {
                    Div({
                        style {
                            fontSize(.9.r)
                            opacity(.7)
                        }
                    }) {
                        Text(people.joinToString { it.name ?: application.appString { someone } })
                    }
                } else {
                    Div({
                        style {
                            fontSize(.9.r)
                            opacity(.7)
                        }
                    }) {
                        Text(application.appString { Strings.collaborators })
                    }
                }
            }

            // Card task status + Mark done
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(1.r)
                }
            }) {
                Div({
                    classes(if (status.isNotBlank()) {
                        listOf(Styles.button, Styles.buttonSmall)
                    } else {
                        listOf(Styles.outlineButton, Styles.outlineButtonSmall)
                    })
                    style {
                        flex(1)
                        if (status.isNotBlank()) {
                            backgroundColor(tagColor(status))
                            color(Color.white)
                        }
                    }
                    onClick {
                        scope.launch {
                            val select: suspend (List<String>) -> Unit = { items ->
                                val newStatus = inputSelectDialog(
                                    confirmButton = application.appString { okay },
                                    placeholder = application.appString { Strings.status },
                                    items = items
                                )
                                if (newStatus != null) {
                                    status = newStatus
                                    statusValue = newStatus
                                }
                            }

                            if (allCards != null) {
                                select(allCards.mapNotNull { it.task?.status }.filter { it.isNotBlank() }.distinct().sorted())
                            } else {
                                api.groupCards(groupId) { cards ->
                                    val items = cards.mapNotNull { it.task?.status }.filter { it.isNotBlank() }.distinct().sorted()
                                    scope.launch {
                                        select(items)
                                    }
                                }
                            }
                        }
                    }
                }) {
                    Text(status.takeIf { it.isNotBlank() } ?: application.appString { selectStatus })
                }

                Label(attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(.5.r)
                        cursor("pointer")
                    }
                }) {
                    CheckboxInput(done) {
                        onInput {
                            done = it.value
                            doneValue = it.value
                        }
                    }
                    Text(application.appString { markAsDone })
                }
            }

            // Card categories
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexWrap(FlexWrap.Wrap)
                    gap(.5.r)
                }
            }) {
                categories.forEach { category ->
                    Span({
                        classes(Styles.button, Styles.buttonSmall)
                        style {
                            backgroundColor(tagColor(category))
                            color(Color.white)
                        }
                        title(application.appString { tapToRemove })
                        onClick {
                            categories = categories - category
                            categoriesValue = categories
                        }
                    }) {
                        Text(category)
                    }
                }
                Div({
                    classes(Styles.outlineButton, Styles.outlineButtonSmall)
                    onClick {
                        scope.launch {
                            val select: suspend (List<String>) -> Unit = { items ->
                                val newCategory = inputSelectDialog(
                                    confirmButton = application.appString { okay },
                                    placeholder = application.appString { Strings.category },
                                    items = items
                                )
                                if (newCategory != null && newCategory !in categories) {
                                    categories = categories + newCategory
                                    categoriesValue = categories
                                }
                            }

                            if (allCards != null) {
                                select(allCards.flatMap { it.categories.orEmpty() }.filter { it.isNotBlank() }.distinct().sorted())
                            } else {
                                api.groupCards(groupId) { cards ->
                                    val items = cards.flatMap { it.categories.orEmpty() }.filter { it.isNotBlank() }.distinct().sorted()
                                    scope.launch {
                                        select(items)
                                    }
                                }
                            }
                        }
                    }
                }) {
                    Text("+ ${application.appString { addCategory }}")
                }
            }

            // Card description
            FlexInput(
                value = description,
                onChange = { description = it; descriptionValue = it },
                placeholder = application.appString { Strings.description },
                singleLine = false
            )
        }
    }

    if (result == true) {
        val ownerId = card?.person ?: application.me.value?.id
        val finalCollaborators = collaboratorsValue.filter { it != ownerId }

        val updatedTask = (card?.task ?: Task()).apply {
            this.status = statusValue
            this.done = doneValue
        }
        val updatedCard = (card ?: Card()).apply {
            this.name = nameValue
            this.conversation = json.encodeToString(card?.getConversation()?.apply {
                message = descriptionValue
            } ?: ConversationItem(message = descriptionValue))
            this.categories = categoriesValue
            this.collaborators = finalCollaborators
            this.task = updatedTask
            this.group = groupId
        }

        if (isNew) {
            api.newCard(updatedCard) {
                onUpdated()
            }
        } else {
            api.updateCard(card.id!!, updatedCard) {
                onUpdated()
            }
        }
    }
}
