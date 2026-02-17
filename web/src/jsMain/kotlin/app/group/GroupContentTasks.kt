package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import api
import app.ailaai.api.groupCards
import app.cards.MapList
import app.components.Empty
import app.components.FlexInput
import app.dialog.batchTasksDialog
import app.dialog.editTaskDialog
import app.nav.CardItem
import appString
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import components.IconButton
import components.Loading
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import tagColor
import com.queatz.db.GroupContent as GroupContentModel

@Composable
fun GroupContentTasks(
    group: GroupExtended
) {
    val scope = rememberCoroutineScope()
    val allCards = remember { MutableStateFlow<List<Card>?>(null) }
    val cards by allCards.collectAsState()
    fun reload() {
        scope.launch {
            api.groupCards(group.group!!.id!!) {
                allCards.value = it
            }
        }
    }
    LaunchedEffect(group) {
        reload()
    }

    if (cards == null) {
        Loading()
    } else {
        var search by remember { mutableStateOf("") }
        var isSearchFocused by remember { mutableStateOf(false) }
        val filteredCards = remember(cards, search) {
            val list = if (search.isBlank()) cards!! else cards!!.filter {
                it.name?.contains(search, ignoreCase = true) == true ||
                        it.content?.contains(search, ignoreCase = true) == true ||
                        it.categories?.any { it.contains(search, ignoreCase = true) } == true ||
                        it.task?.let { task ->
                            task.status?.contains(search, ignoreCase = true) == true ||
                                    task.fields?.any {
                                        it.value.contains(
                                            search,
                                            ignoreCase = true
                                        )
                                    } == true
                        } == true
            }
            list.sortedWith(
                compareBy<Card> { it.task?.done ?: false }
                    .thenByDescending { it.createdAt }
            )
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                gap(.5.r)
            }
        }) {
            Div({
                style { flex(1) }
            }) {
                FlexInput(
                    value = search,
                    onChange = { search = it },
                    placeholder = appString { this.search },
                    onDismissRequest = {
                        search = ""
                    },
                    onFocus = {
                        isSearchFocused = true
                    },
                    onBlur = {
                        scope.launch {
                            delay(200)
                            isSearchFocused = false
                        }
                    }
                )
            }
            IconButton("add", appString { newTask }, styles = {
                marginRight(.5.r)
            }) {
                scope.launch {
                    editTaskDialog(
                        groupId = group.group!!.id!!,
                        allCards = cards,
                        initialName = search
                    ) {
                        reload()
                    }
                }
            }
        }

        if (isSearchFocused) {
            val statuses = remember(cards) {
                cards!!.mapNotNull { it.task?.status }.filter { it.isNotBlank() }.distinct().sorted()
            }
            val categories = remember(cards) {
                cards!!.flatMap { it.categories.orEmpty() }.filter { it.isNotBlank() }.distinct().sorted()
            }

            if (statuses.isNotEmpty() || categories.isNotEmpty()) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        overflow("auto")
                        gap(.5.r)
                        marginTop(.5.r)
                        flexShrink(0)
                        padding(0.5.r, 0.r)
                    }
                }) {
                    statuses.forEach { status ->
                        Span({
                            classes(Styles.button, Styles.buttonSmall)
                            style {
                                backgroundColor(tagColor(status))
                                color(Color.white)
                                flexShrink(0)
                            }
                            onMouseDown { it.preventDefault() }
                            onClick {
                                search = status
                            }
                        }) {
                            Text(status)
                        }
                    }
                    categories.forEach { category ->
                        Span({
                            classes(Styles.button, Styles.buttonSmall)
                            style {
                                backgroundColor(tagColor(category))
                                color(Color.white)
                                flexShrink(0)
                            }
                            onMouseDown { it.preventDefault() }
                            onClick {
                                search = category
                            }
                        }) {
                            Text(category)
                        }
                    }
                }
            }
        }

        if (filteredCards.isEmpty()) {
            Empty { Text(if (search.isBlank()) "No tasks." else "No results.") }
        } else {
            MapList(
                cards = filteredCards,
                allCards = cards,
                showPhoto = false,
                people = group.members?.mapNotNull { it.person },
                groupId = group.group!!.id!!,
                onUpdated = { reload() },
                styles = {
                    marginTop(1.r)
                }
            ) { card ->
                scope.launch {
                    editTaskDialog(group.group!!.id!!, card, cards) {
                        reload()
                    }
                }
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                marginTop(1.r)
            }
        }) {
            IconButton("fact_check", appString { batch }, appString { batch }) {
                scope.launch {
                    batchTasksDialog(
                        groupId = group.group!!.id!!,
                        initialCards = filteredCards,
                        allCards = allCards,
                        people = group.members?.mapNotNull { it.person }
                    ) {
                        reload()
                    }
                }
            }
        }
    }
}
