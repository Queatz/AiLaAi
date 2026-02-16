package app.dialog

import Strings
import Styles
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppNavigation
import app.ailaai.api.cardPeople
import app.ailaai.api.groupCards
import app.ailaai.api.newCard
import app.ailaai.api.updateCard
import app.appNav
import app.components.FlexInput
import app.group.friendsDialog
import application
import com.queatz.db.Card
import com.queatz.db.ConversationItem
import com.queatz.db.Person
import com.queatz.db.Task
import components.GroupPhoto
import components.GroupPhotoItem
import components.IconButton
import components.getConversation
import ellipsize
import json
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import tagColor

suspend fun editTaskDialog(
    groupId: String,
    card: Card? = null,
    allCards: List<Card>? = null,
    initialName: String? = null,
    initialDescription: String? = null,
    onUpdated: () -> Unit,
) {
    var nameValue = initialName ?: card?.name ?: ""
    var descriptionValue = initialDescription ?: card?.getConversation()?.message ?: ""
    var statusValue = card?.task?.status ?: ""
    var doneValue = card?.task?.done ?: false
    var categoriesValue = card?.categories ?: emptyList<String>()
    var collaboratorsValue = card?.collaborators ?: emptyList<String>()
    var fieldsValue = card?.task?.fields ?: emptyMap<String, String>()

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
        var fields by remember { mutableStateOf(fieldsValue) }
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

            // Custom fields
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(.5.r)
                }
            }) {
                fields.forEach { (fieldName, fieldValue) ->
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            gap(1.r)
                            cursor("pointer")
                        }
                        onClick {
                            scope.launch {
                                val select: suspend (List<String>) -> Unit = { items ->
                                    val newValue = inputSelectDialog(
                                        confirmButton = application.appString { okay },
                                        placeholder = fieldName,
                                        defaultValue = fieldValue,
                                        items = items,
                                        extraButtons = { resolve ->
                                            IconButton("delete", application.appString { delete }) {
                                                fields = fields - fieldName
                                                fieldsValue = fields
                                                resolve(false)
                                            }
                                        }
                                    )
                                    if (newValue != null) {
                                        fields = fields + (fieldName to newValue)
                                        fieldsValue = fields
                                    }
                                }

                                if (allCards != null) {
                                    select(allCards.mapNotNull { it.task?.fields?.get(fieldName) }.filter { it.isNotBlank() }.distinct().sorted())
                                } else {
                                    api.groupCards(groupId) { cards ->
                                        val items = cards.mapNotNull { it.task?.fields?.get(fieldName) }.filter { it.isNotBlank() }.distinct().sorted()
                                        scope.launch {
                                            select(items)
                                        }
                                    }
                                }
                            }
                        }
                    }) {
                        Div({
                            style {
                                flex(1)
                                opacity(.7)
                                fontSize(.9.r)
                                width(0.r)
                                ellipsize()
                            }
                        }) {
                            Text(fieldName)
                        }
                        Div({
                            style {
                                flex(1)
                                fontWeight("bold")
                                width(0.r)
                                ellipsize()
                            }
                        }) {
                            Text(fieldValue)
                        }
                    }
                }

                Div({
                    classes(Styles.outlineButton, Styles.outlineButtonSmall)
                    style {
                        alignSelf(AlignSelf.FlexStart)
                    }
                    onClick {
                        scope.launch {
                            val select: suspend (List<String>) -> Unit = { items ->
                                val fieldName = inputSelectDialog(
                                    confirmButton = application.appString { okay },
                                    placeholder = application.appString { Strings.field },
                                    items = items
                                )
                                if (!fieldName.isNullOrBlank()) {
                                    scope.launch {
                                        val selectValue: suspend (List<String>) -> Unit = { valueItems ->
                                            val fieldValue = inputSelectDialog(
                                                confirmButton = application.appString { okay },
                                                placeholder = fieldName,
                                                items = valueItems
                                            )
                                            if (fieldValue != null) {
                                                fields = fields + (fieldName to fieldValue)
                                                fieldsValue = fields
                                            }
                                        }

                                        if (allCards != null) {
                                            selectValue(allCards.mapNotNull { it.task?.fields?.get(fieldName) }.filter { it.isNotBlank() }.distinct().sorted())
                                        } else {
                                            api.groupCards(groupId) { cards ->
                                                val valueItems = cards.mapNotNull { it.task?.fields?.get(fieldName) }.filter { it.isNotBlank() }.distinct().sorted()
                                                scope.launch {
                                                    selectValue(valueItems)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (allCards != null) {
                                select(allCards.flatMap { it.task?.fields?.keys.orEmpty() }.filter { it.isNotBlank() }.distinct().sorted())
                            } else {
                                api.groupCards(groupId) { cards ->
                                    val items = cards.flatMap { it.task?.fields?.keys.orEmpty() }.filter { it.isNotBlank() }.distinct().sorted()
                                    scope.launch {
                                        select(items)
                                    }
                                }
                            }
                        }
                    }
                }) {
                    Text("+ ${application.appString { Strings.addField }}")
                }
            }
        }
    }

    if (result == true) {
        val ownerId = card?.person ?: application.me.value?.id
        val finalCollaborators = collaboratorsValue.filter { it != ownerId }

        val updatedTask = (card?.task ?: Task()).apply {
            this.status = statusValue
            this.done = doneValue
            this.fields = fieldsValue
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
