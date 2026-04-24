package app.group

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.groupCards
import app.cards.MapList
import app.components.Empty
import app.components.FlexInput
import app.compose.rememberDarkMode
import app.dialog.batchTasksDialog
import app.dialog.editTaskDialog
import app.dialog.inputSelectDialog
import app.menu.Menu
import appString
import application
import getString
import Strings
import LocalConfiguration
import lib.ResizeObserver
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import components.Icon
import components.IconButton
import components.Loading
import format
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Progress
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import org.w3c.dom.DOMRect
import r
import tagColor

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
    LaunchedEffect(group.group?.id) {
        reload()
    }

    if (cards == null) {
        Loading()
    } else {
        var search by remember { mutableStateOf("") }
        var isSearchFocused by remember { mutableStateOf(false) }
        var showSubtasks by remember { mutableStateOf(false) }
        var showDone by remember { mutableStateOf(true) }
        var sortByField by remember { mutableStateOf<String?>(null) }
        var filterByField by remember { mutableStateOf<String?>(null) }
        var filterByValue by remember { mutableStateOf<String?>(null) }
        var expandedCardId by remember { mutableStateOf<String?>(null) }
        var showColumns by remember { mutableStateOf(false) }
        var contentWidth by remember { mutableStateOf(0) }
        val isDarkMode = rememberDarkMode()
        val configuration = LocalConfiguration.current

        val filteredCards = remember(cards, search, showSubtasks, showDone, sortByField, filterByField, filterByValue) {
            val search = search.trim()
            var list = if (search.isBlank()) cards!! else cards!!.filter {
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
            if (!showSubtasks) {
                list = list.filter { it.task?.owner == null }
            }
            if (!showDone) {
                list = list.filter { it.task?.done != true }
            }
            filterByField?.let { field ->
                filterByValue?.let { value ->
                    list = list.filter { it.task?.fields?.get(field) == value }
                }
            }

            list.sortedWith(
                if (sortByField != null) {
                    compareBy<Card> { it.task?.fields?.get(sortByField) ?: Char.MAX_VALUE.toString() }
                        .thenBy { it.task?.done ?: false }
                        .thenByDescending { it.createdAt }
                } else {
                    compareBy<Card> { it.task?.done ?: false }
                        .thenByDescending { it.createdAt }
                }
            )
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                flex(1)
            }
            ref {
                contentWidth = it.clientWidth

                val observer = ResizeObserver { _, _ ->
                    contentWidth = it.clientWidth
                }.apply {
                    observe(it)
                }

                onDispose {
                    observer.disconnect()
                }
            }
        }) {
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
                        useDefaultWidth = false,
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
                IconButton("add", appString { newTask }) {
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
            var showMenu by remember { mutableStateOf(false) }
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }

            IconButton(
                "tune",
                appString { options },
                count = (if (sortByField != null) 1 else 0) + (if (filterByField != null) 1 else 0),
                background = showMenu,
                styles = {
                    marginRight(.5.r)
                }
            ) {
                menuTarget = (it.target as HTMLElement).getBoundingClientRect()
                showMenu = true
            }

            if (showMenu && menuTarget != null) {
                val okayStr = getString(Strings.okay, configuration.language)
                val sortByStr = getString(Strings.sortBy, configuration.language)
                val filterByStr = getString(Strings.filterBy, configuration.language)

                Menu(
                    onDismissRequest = { showMenu = false },
                    target = menuTarget!!
                ) {
                    item(
                        title = appString { subtasks },
                        selected = showSubtasks,
                        icon = if (showSubtasks) "check_box" else "check_box_outline_blank"
                    ) {
                        showSubtasks = !showSubtasks
                    }
                    item(
                        title = appString { showDoneTasks },
                        selected = showDone,
                        icon = if (showDone) "check_box" else "check_box_outline_blank"
                    ) {
                        showDone = !showDone
                    }
                    item(
                        title = appString { columns },
                        selected = showColumns,
                        icon = if (showColumns) "view_column" else "view_agenda"
                    ) {
                        showColumns = !showColumns
                    }
                    item(
                        title = appString { sortBy },
                        description = sortByField ?: appString { none },
                        selected = sortByField != null,
                        icon = "sort"
                    ) {
                        scope.launch {
                            val fields = cards?.flatMap { it.task?.fields?.keys ?: emptySet() }?.distinct()?.sorted() ?: emptyList()
                            val field = inputSelectDialog(
                                confirmButton = okayStr,
                                items = fields,
                                placeholder = sortByStr
                            )
                            if (field != null) {
                                sortByField = if (field.isBlank()) null else field
                            }
                        }
                    }
                    item(
                        title = appString { filterBy },
                        description = if (filterByField != null) "$filterByField: $filterByValue" else appString { none },
                        selected = filterByField != null,
                        icon = "filter_alt"
                    ) {
                        scope.launch {
                            val fields = cards?.flatMap { it.task?.fields?.keys ?: emptySet() }?.distinct()?.sorted() ?: emptyList()
                            val field = inputSelectDialog(
                                confirmButton = okayStr,
                                items = fields,
                                placeholder = filterByStr
                            )
                            if (field != null) {
                                if (field.isBlank()) {
                                    filterByField = null
                                    filterByValue = null
                                } else {
                                    val values = cards?.mapNotNull { it.task?.fields?.get(field) }?.filter { it.isNotBlank() }?.distinct()?.sorted() ?: emptyList()
                                    val value = inputSelectDialog(
                                        confirmButton = okayStr,
                                        items = values,
                                        placeholder = field
                                    )
                                    if (value != null) {
                                        filterByField = field
                                        filterByValue = value
                                    }
                                }
                            }
                        }
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
                            title(application.appString { this.status })
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
                            title(application.appString { this.category })
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
        } else if (showColumns) {
            val statusGroups = remember(filteredCards) {
                filteredCards.groupBy { it.task?.status ?: "" }
                    .toList()
                    .sortedByDescending { it.second.size }
            }
            var selectedStatus by remember(statusGroups) { mutableStateOf(statusGroups.firstOrNull()?.first ?: "") }

            if (contentWidth < 480) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(.5.r)
                        overflowX("auto")
                        padding(.5.r, 0.r)
                        marginTop(1.r)
                    }
                }) {
                    statusGroups.forEach { (status, cards) ->
                        val isSelected = selectedStatus == status
                        Span({
                            classes(Styles.button, Styles.buttonSmall)
                            style {
                                backgroundColor(if (isSelected) tagColor(status) else (if (isDarkMode) rgba(255, 255, 255, 0.1) else rgba(0, 0, 0, 0.1)))
                                color(if (isSelected) Color.white else (if (isDarkMode) Color.white else Color.black))
                                flexShrink(0)
                            }
                            onClick { selectedStatus = status }
                        }) {
                            Text("${if (status.isBlank()) appString { noStatus } else status} (${cards.size})")
                        }
                    }
                }

                MapList(
                    cards = statusGroups.find { it.first == selectedStatus }?.second ?: emptyList(),
                    allCards = cards,
                    showPhoto = false,
                    showStatus = false,
                    people = group.members?.mapNotNull { it.person },
                    groupId = group.group!!.id!!,
                    expandedCardId = expandedCardId,
                    onExpanded = { card, expanded ->
                        expandedCardId = if (expanded) card.id else null
                    },
                    onUpdated = { reload() },
                    styles = {
                        marginTop(1.r)
                    },
                    onBackground = true,
                ) { card ->
                    scope.launch {
                        editTaskDialog(group.group!!.id!!, card, cards) {
                            reload()
                        }
                    }
                }
            } else {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        gap(1.r)
                        overflowX("auto")
                        marginTop(1.r)
                        alignItems(AlignItems.FlexStart)
                    }
                }) {
                    statusGroups.forEach { (status, cardsForStatus) ->
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                flex(1)
                                minWidth(300.px)
                            }
                        }) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    alignItems(AlignItems.Center)
                                    justifyContent(JustifyContent.SpaceBetween)
                                    padding(.5.r, 1.r)
                                    borderRadius(.5.r)
                                    backgroundColor(tagColor(status))
                                    color(Color.white)
                                    marginBottom(.5.r)
                                }
                            }) {
                                Span({ style { fontWeight("bold") } }) {
                                    Text(if (status.isBlank()) appString { noStatus } else status)
                                }
                                Span({ style { opacity(.8); fontSize(.9.r) } }) {
                                    Text(cardsForStatus.size.toString())
                                }
                            }

                            MapList(
                                cards = cardsForStatus,
                                allCards = cards,
                                showPhoto = false,
                                showStatus = false,
                                people = group.members?.mapNotNull { it.person },
                                groupId = group.group!!.id!!,
                                expandedCardId = expandedCardId,
                                onExpanded = { card, expanded ->
                                    expandedCardId = if (expanded) card.id else null
                                },
                                onUpdated = { reload() },
                                onBackground = true,
                            ) { card ->
                                scope.launch {
                                    editTaskDialog(group.group!!.id!!, card, cards) {
                                        reload()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredCards.size > 10) {
                val total = filteredCards.size
                val done = filteredCards.count { it.task?.done == true }
                val remaining = total - done
                val percent = if (total > 0) (done.toFloat() / total.toFloat() * 100).toInt() else 0
                val statusCounts = filteredCards.groupBy { it.task?.status ?: "" }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }

                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(1.r)
                        padding(1.r)
                        marginTop(1.r)
                        property(
                            "background-color",
                            (if (isDarkMode) Styles.colors.dark.surface else Styles.colors.surface).toString() + "80"
                        )
                        borderRadius(1.r)
                        property("backdrop-filter", "blur(6px)")
                        if (isDarkMode) {
                            color(Color.white)
                        }
                    }
                }) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            gap(.5.r)
                        }
                    }) {
                        Icon("analytics")
                        Span({
                            style {
                                fontWeight("bold")
                                fontSize(1.2.r)
                            }
                        }) {
                            Text(appString { showingNTasks }.format(total.toString()))
                        }
                    }

                    if (percent in 1..<100) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                gap(1.r)
                                flexWrap(FlexWrap.Wrap)
                                alignItems(AlignItems.Center)
                            }
                        }) {
                            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(.25.r) } }) {
                                Icon("check_circle", styles = { color(Styles.colors.green); fontSize(1.2.r) })
                                Text(appString { nTasksDone }.format(done.toString()))
                            }
                            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(.25.r) } }) {
                                Icon("pending", styles = { opacity(.5); fontSize(1.2.r) })
                                Text(appString { nTasksToGo }.format(remaining.toString()))
                            }
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    alignItems(AlignItems.Center)
                                    gap(.25.r)
                                    if (percent == 100) color(Styles.colors.green)
                                }
                            }) {
                                Text(appString { percentComplete }.format(percent.toString()))
                            }
                        }

                        Progress({
                            attr("value", done.toString())
                            attr("max", total.toString())
                            style {
                                width(100.percent)
                                height(.5.r)
                                borderRadius(.25.r)
                                property("accent-color", Styles.colors.primary.toString())
                            }
                        })
                    }

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            gap(.5.r)
                            flexWrap(FlexWrap.Wrap)
                            marginTop(.5.r)
                        }
                    }) {
                        statusCounts.forEach { (status, count) ->
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    alignItems(AlignItems.Center)
                                    backgroundColor(if (isDarkMode) rgba(255, 255, 255, 0.05) else rgba(0, 0, 0, 0.05))
                                    borderRadius(1.r)
                                    paddingRight(.75.r)
                                }
                            }) {
                                Span({
                                    classes(Styles.button, Styles.buttonSmall)
                                    style {
                                        backgroundColor(if (status.isBlank()) Styles.colors.gray else tagColor(status))
                                        color(Color.white)
                                        whiteSpace("nowrap")
                                        flexShrink(0)
                                        cursor("default")
                                        property("pointer-events", "none")
                                        marginRight(.5.r)
                                    }
                                }) {
                                    Text(if (status.isBlank()) appString { noStatus } else status)
                                }
                                Span({
                                    style {
                                        fontWeight("bold")
                                        fontSize(.9.r)
                                        opacity(.8)
                                    }
                                }) {
                                    Text(count.toString())
                                }
                            }
                        }
                    }
                }
            }

            MapList(
                cards = filteredCards,
                allCards = cards,
                showPhoto = false,
                people = group.members?.mapNotNull { it.person },
                groupId = group.group!!.id!!,
                expandedCardId = expandedCardId,
                onExpanded = { card, expanded ->
                    expandedCardId = if (expanded) card.id else null
                },
                onUpdated = { reload() },
                styles = {
                    marginTop(1.r)
                },
                onBackground = true,
            ) { card ->
                scope.launch {
                    editTaskDialog(group.group!!.id!!, card, cards) {
                        reload()
                    }
                }
            }
        }

        if (filteredCards.isNotEmpty()) {
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
    }
}
