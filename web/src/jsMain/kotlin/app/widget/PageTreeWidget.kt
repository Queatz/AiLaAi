package app.widget

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
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.newCard
import app.ailaai.api.updateCard
import app.cards.NewCardInput
import app.components.Empty
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.inputSelectDialog
import app.menu.Menu
import app.messaages.inList
import app.nav.NavSearchInput
import app.softwork.routingcompose.Router
import appString
import application
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.PageTreeData
import components.Icon
import components.IconButton
import components.getConversation
import isMine
import json
import kotlinx.browser.window
import kotlinx.coroutines.launch
import lib.toLocaleString
import notBlank
import notEmpty
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import updateWidget
import widget
import kotlin.random.Random

sealed class TagFilter {
    data class Tag(val tag: String) : TagFilter()
    data object Untagged : TagFilter()
}

internal fun tagColor(tag: String): CSSColorValue {
    val hue = Random(tag.hashCode()).nextInt(360)
    return Color("hsl($hue, 60%, 40%)")
}

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
    var tagFilters by remember(widgetId) {
        mutableStateOf(emptySet<TagFilter>())
    }
    var stageFilters by remember(widgetId) {
        mutableStateOf(emptySet<TagFilter>())
    }
    var data by remember(widgetId) {
        mutableStateOf<PageTreeData?>(null)
    }
    val searchedCards = remember(cards, search) {
        if (search.isNotBlank()) {
            cards.filter {
                it.name?.contains(search, ignoreCase = true) == true
            }
        } else {
            cards
        }
    }
    val stageCount = remember(data, searchedCards) {
        data?.stages?.filterKeys { id ->
            searchedCards.any { it.id == id }
        }?.values?.groupingBy { it }?.eachCount()
    }
    val noStageCount = remember(searchedCards, data) {
        val stagedCards = data?.stages?.entries?.filter { it.value.isNotEmpty() }?.map { it.key } ?: emptyList()

        cards.count { card ->
            card.id !in stagedCards && searchedCards.any { card.id == it.id }
        }
    }
    val allStages = remember(data, searchedCards) {
        data?.stages?.filterKeys { id ->
            searchedCards.any { it.id == id }
        }?.values?.distinct()?.sorted()?.sortedDescending()
    }
    val stagedCards = remember(searchedCards, stageFilters) {
        if (search.isNotBlank()) {
            cards.filter {
                it.name?.contains(search, ignoreCase = true) == true
            }
        } else {
            cards
        }.let {
            val stages = stageFilters.map {
                when (it) {
                    is TagFilter.Tag -> it.tag
                    is TagFilter.Untagged -> null
                }
            }
            when {
                stages.isNotEmpty() -> {
                    it.filter {
                        data?.stages?.get(it.id!!) in stages
                    }
                }

                else -> {
                    it
                }
            }
        }
    }
    val tagCount = remember(data, stagedCards) {
        data?.tags?.filterKeys { id ->
            stagedCards.any { it.id == id }
        }?.values?.flatten()?.groupingBy { it }?.eachCount()
    }
    val noTagCount = remember(stagedCards, data) {
        val taggedCards = data?.tags?.entries?.filter { it.value.isNotEmpty() }?.map { it.key } ?: emptyList()

        stagedCards.count { card ->
            card.id !in taggedCards && stagedCards.any { card.id == it.id }
        }
    }
    val allTags = remember(data, stagedCards) {
        data?.tags?.filterKeys { id ->
            stagedCards.any { it.id == id }
        }?.values
            ?.asSequence()
            ?.flatten()
            ?.distinct()
            ?.sortedWith(
                compareByDescending<String> { tag ->
                    stagedCards.sumOf { card ->
                        data?.votes?.get(card.id!!)
                            ?.takeIf { data?.tags?.get(card.id!!)?.contains(tag) == true }
                            ?: 0
                    }
                }
                    .thenByDescending { tagCount?.get(it) ?: 0 }
                    .thenBy {
                        it
                    }
            )
            ?.toList()
    }
    val shownCards = remember(stagedCards, tagFilters) {
        stagedCards.let {
            val tags = tagFilters.map {
                when (it) {
                    is TagFilter.Tag -> it.tag
                    is TagFilter.Untagged -> null
                }
            }
            when {
                tags.isNotEmpty() -> {
                    it.filter {
                        data?.tags?.get(it.id!!).orEmpty().let {
                            it.any { it in tags } || (it.isEmpty() && null in tags)
                        }
                    }
                }

                else -> {
                    it
                }
            }
        }
    }

    var isEditingTagCategories by remember(widgetId) {
        mutableStateOf(false)
    }

    var showAll by remember(widgetId) {
        mutableStateOf(false)
    }

    var tagMenuTarget by remember(widgetId) {
        mutableStateOf<DOMRect?>(null)
    }

    var newPageTags by remember {
        mutableStateOf(emptyList<String>())
    }

    tagMenuTarget?.let {
        // todo: this menu is still offset incorrectly because of the page header
        Menu(
            onDismissRequest = {
                tagMenuTarget = null
            },
            target = it,
            useOffsetParent = true
        ) {
            item(
                // todo: translate
                title = "Edit categories",
            ) {
                isEditingTagCategories = !isEditingTagCategories
            }
        }
    }

    suspend fun reload() {
        api.cardsCards(data?.card ?: return) {
            cards = it
        }
    }

    suspend fun saveCard(cardId: String, card: Card) {
        api.updateCard(cardId, card) {
            reload()
        }
    }

    suspend fun saveConversation(card: Card, value: String) {
        val conversation = card.getConversation()
        conversation.message = value
        saveCard(card.id!!, Card(conversation = json.encodeToString(conversation)))
    }

    suspend fun addTagsToCard(card: Card, tags: List<String>) {
        api.updateWidget(
            id = widgetId,
            widget = Widget(
                data = json.encodeToString(
                    data!!.copy(
                        tags = data!!.tags.toMutableMap().apply {
                            put(card.id!!, (getOrElse(card.id!!) { emptyList() } + tags).distinct())
                        }
                    )
                )
            )
        ) {
            widget = it
            data = json.decodeFromString<PageTreeData>(it.data!!)
        }
    }

    fun newSubCard(inCardId: String, name: String, active: Boolean) {
        val newPageTags = newPageTags
        scope.launch {
            api.newCard(Card(name = name, parent = inCardId, active = active)) {
                reload()
                if (newPageTags.isNotEmpty()) {
                    addTagsToCard(it, newPageTags)
                }
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

    fun removeTag(card: Card, tag: String) {
        scope.launch {
            api.updateWidget(
                widgetId,
                Widget(data = json.encodeToString(data!!.copy(tags = data!!.tags.toMutableMap().apply {
                    put(card.id!!, getOrElse(card.id!!) { emptyList() } - tag)
                })))
            ) {
                widget = it
                data = json.decodeFromString<PageTreeData>(it.data!!)
            }
        }
    }

    fun setStage(card: Card) {
        scope.launch {
            val stage = inputSelectDialog(
                // todo: translate
                confirmButton = "Update",
                items = allStages
            )

            if (stage != null) {
                api.updateWidget(
                    widgetId,
                    Widget(data = json.encodeToString(data!!.copy(stages = data!!.stages.toMutableMap().apply {
                        if (stage.isNotBlank()) {
                            put(card.id!!, stage.trim())
                        } else {
                            remove(card.id!!)
                        }
                    })))
                ) {
                    widget = it
                    data = json.decodeFromString<PageTreeData>(it.data!!)
                }
            }
        }
    }

    fun selectTag(onTag: suspend (String) -> Unit) {
        scope.launch {
            val tag = inputSelectDialog(
                // todo: translate
                confirmButton = "Add tag",
                items = data?.tags?.values?.flatten().orEmpty().distinct().sorted(),
                itemStyle = { tag ->
                    backgroundColor(tagColor(tag))
                }
            )

            if (!tag.isNullOrBlank()) {
                onTag(tag)
            }
        }
    }

    fun addTag(card: Card) {
        selectTag { tag ->
            addTagsToCard(card, tag.inList())
        }
    }

    fun setTagCategory(tag: String) {
        scope.launch {
            val category = inputSelectDialog(
                // todo: translate
                confirmButton = "Set category",
                items = data?.categories?.values.orEmpty()
                    .mapNotNull { it.firstOrNull() }.distinct().sorted(),
                itemStyle = { tag ->
                    backgroundColor(tagColor(tag))
                }
            )

            if (!category.isNullOrBlank()) {
                api.updateWidget(
                    id = widgetId,
                    widget = Widget(
                        data = json.encodeToString(
                            data!!.copy(
                                categories = data!!.categories.toMutableMap().apply {
                                    put(tag, category.inList())
                                }
                            )
                        )
                    )
                ) {
                    widget = it
                    data = json.decodeFromString<PageTreeData>(it.data!!)
                }
            }
        }
    }

    fun addMultiple() {
        scope.launch {
            val text = inputDialog(
                // todo: translate
                title = "Add multiple",
                singleLine = false,
                // todo: translate
                placeholder = "Paste one page per line. Indented lines (2+ spaces or tabs) will be used as descriptions.",
                // todo: translate
                confirmButton = "Review"
            )

            if (text.isNullOrBlank()) return@launch

            // Parse the input text to identify cards and their descriptions
            val cards = mutableListOf<Pair<String, String?>>()
            var currentCard: String? = null
            var currentDescription = StringBuilder()

            text.lines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isBlank()) {
                    // Skip blank lines
                    return@forEach
                }

                // Check if this is a description (indented line)
                val isDescription = line.startsWith("  ") || line.startsWith("\t")

                if (isDescription && currentCard != null) {
                    // Add to the current card's description
                    if (currentDescription.isNotEmpty()) {
                        currentDescription.append("\n")
                    }
                    currentDescription.append(trimmedLine)
                } else {
                    // Save the previous card if there was one
                    currentCard?.let { cardName ->
                        cards.add(Pair(cardName, if (currentDescription.isNotEmpty()) currentDescription.toString() else null))
                        currentDescription = StringBuilder()
                    }
                    // Start a new card
                    currentCard = trimmedLine
                }
            }

            // Add the last card if there is one
            currentCard?.let { cardName ->
                cards.add(Pair(cardName, if (currentDescription.isNotEmpty()) currentDescription.toString() else null))
            }

            if (cards.isEmpty()) return@launch

            // Show a preview of the cards to be created
            val confirmed = dialog(
                // todo: translate
                title = "Review",
                // todo: translate
                confirmButton = "${cards.size} pages will be created",
                cancelButton = application.appString { cancel }
            ) { resolve ->
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        gap(1.r)
                    }
                }) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            gap(.5.r)
                            maxHeight(20.r)
                            overflow("auto")
                        }
                    }) {
                        cards.forEach { (name, description) ->
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    padding(.5.r)
                                    borderRadius(.25.r)
                                }
                            }) {
                                Div({
                                    style {
                                        fontWeight("bold")
                                    }
                                }) {
                                    Text(name)
                                }

                                description?.let {
                                    Div({
                                        style {
                                            fontSize(14.px)
                                            opacity(.85)
                                        }
                                    }) {
                                        Text(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (confirmed == true) {
                // Create cards one by one via the API
                cards.forEach { (name, description) ->
                    api.newCard(Card(
                        name = name,
                        parent = data?.card ?: return@forEach,
                        active = true
                    )) { card ->
                        // If there's a description, update the card with it
                        if (description != null) {
                            saveConversation(card, description)
                        }

                        // Add tags to the card if there are any
                        if (newPageTags.isNotEmpty()) {
                            addTagsToCard(card, newPageTags)
                        }
                    }
                }

                // Refresh the page
                reload()
            }
        }
    }

    Div(
        {
            classes(WidgetStyles.pageTree)
        }
    ) {
        if (isMine) {
            Div(
                {
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(1.r)
                    }
                }
            ) {
                NewCardInput(defaultMargins = false, styles = {
                    flexGrow(1)
                }) { name, active ->
                    newSubCard(data?.card ?: return@NewCardInput, name, active)
                }

                IconButton(
                    name = "list",
                    title = appString { multiple },
                    background = true,
                    onClick = {
                        addMultiple()
                    }
                )
            }

            Tags(
                tags = newPageTags,
                // todo: translate
                title = if (me != null) "Tap to remove" else "",
                onClick = { tag, _ ->
                    newPageTags = newPageTags.filter {
                        it != (tag as? TagFilter.Tag)?.tag
                    }
                }
            ) {
                Button(
                    {
                        classes(Styles.outlineButton)

                        style {
                            padding(0.r, 1.5.r)
                            height(2.5.r)
                        }

                        // todo: translate
                        title("Add tag")

                        onClick {
                            it.stopPropagation()
                            selectTag { tag ->
                                newPageTags += tag
                            }
                        }
                    }
                ) {
                    Icon("new_label") {
                        marginRight(0.r)
                    }
                }
            }
        }

        if (cards.size > 5) {
            NavSearchInput(
                value = search,
                onChange = { search = it },
                defaultMargins = false,
                autoFocus = false,
                styles = {
                    width(100.percent)
                    marginBottom(1.r)
                }
            )

            allStages?.notEmpty?.let { stages ->
                PageTreeHeader(
                    // todo: translate
                    title = "Stages"
                )

                Div({
                    style {
                        marginTop(1.r)
                        display(DisplayStyle.Flex)
                        flexWrap(FlexWrap.Wrap)
                        gap(.5.r)
                    }
                }) {
                    stages.forEach { stage ->
                        val tags = stageFilters.filterIsInstance<TagFilter.Tag>().map { it.tag }

                        TagButton(
                            tag = stage,
                            // todo: translate
                            title = "Tap to filter",
                            selected = stage in tags,
                            outline = true,
                            count = stageCount?.get(stage)?.toString() ?: "",
                            onClick = { multiselect ->
                                if (!multiselect) {
                                    stageFilters = if (TagFilter.Tag(stage) in stageFilters) {
                                        emptySet()
                                    } else {
                                        setOf(TagFilter.Tag(stage))
                                    }
                                } else {
                                    if (stage in tags) {
                                        stageFilters -= TagFilter.Tag(stage)
                                    } else {
                                        stageFilters += TagFilter.Tag(stage)
                                    }
                                }
                            }
                        )
                    }
                    if (stages.isNotEmpty()) {
                        TagButton(
                            // todo: Translate
                            tag = "New",
                            count = noStageCount.toString(),
                            // todo: Translate
                            title = "Tap to filter",
                            selected = TagFilter.Untagged in stageFilters,
                            outline = true,
                            onClick = { multiselect ->
                                stageFilters = if (!multiselect) {
                                    if (TagFilter.Untagged in stageFilters) {
                                        emptySet()
                                    } else {
                                        setOf(TagFilter.Untagged)
                                    }
                                } else {
                                    if (TagFilter.Untagged in stageFilters) {
                                        stageFilters - TagFilter.Untagged
                                    } else {
                                        stageFilters + TagFilter.Untagged
                                    }
                                }
                            }
                        )
                    }
                }
            }

            PageTreeHeader(
                // todo: translate
                title = "Tags"
            ) {
                if (isEditingTagCategories) {
                    IconButton(
                        name = "clear",
                        // todo: translate
                        title = "Leave",
                        small = true,
                        styles = {
                            padding(0.r)
                        }
                    ) {
                        isEditingTagCategories = false
                    }
                } else {
                    IconButton(
                        name = "more_vert",
                        // todo: translate
                        title = "Menu",
                        small = true,
                        styles = {
                            padding(0.r)
                        }
                    ) {
                        tagMenuTarget = if (tagMenuTarget == null) {
                            (it.target as HTMLElement).getBoundingClientRect()
                        } else {
                            null
                        }
                    }
                }
            }

            val allCategories: List<String?> = remember(data) {
                data?.categories?.values?.mapNotNull {
                    it.firstOrNull()
                }?.distinct().orEmpty() + null
            }

            val votesInCategory = remember(data, stagedCards, allCategories) {
                buildMap<String?, Int> {
                    allCategories.forEach { category ->
                        put(
                            key = category,
                            value = stagedCards.filter { card ->
                                val cardCategories = data?.tags?.get(card.id!!)
                                    ?.map { tag ->
                                        data?.categories?.get(tag)?.firstOrNull()
                                    }

                                if (cardCategories.isNullOrEmpty()) {
                                    category == null
                                } else {
                                    cardCategories.any { it == category }
                                }
                            }.sumOf { card ->
                                data?.votes
                                    ?.get(card.id!!)
                                    ?: 0
                            }
                        )
                    }
                }
            }

            val cardsInCategory = remember(data, stagedCards, allCategories) {
                buildMap<String?, Int> {
                    allCategories.forEach { category ->
                        put(
                            key = category,
                            value = stagedCards.count { card ->
                                val cardCategories = data?.tags?.get(card.id!!)
                                    ?.map { tag ->
                                        data?.categories?.get(tag)?.firstOrNull()
                                    }

                                if (cardCategories.isNullOrEmpty()) {
                                    category == null
                                } else {
                                    cardCategories.any { it == category }
                                }
                            }
                        )
                    }
                }
            }

            allTags?.notEmpty?.let { tags ->
                val tagCategories = tags.groupBy { tag ->
                    data?.categories?.get(tag)?.firstOrNull()
                }.filter { (category, tags) ->
                    category == null || tags.isNotEmpty()
                }.entries.sortedWith(
                    compareByDescending<Map.Entry<String?, List<String>>> { (category, _) ->
                        votesInCategory[category] ?: 0
                    }.thenBy {
                        it.key ?: "Zzzzzzzzz" // Uncategorized last
                    }
                )
                tagCategories.forEach { (category, tags) ->
                    Div {
                        if (tagCategories.size > 1) {
                            Div({
                                style {
                                    marginBottom(.5.r)
                                }
                            }) {
                                // todo: translate
                                Text(category ?: "Uncategorized")
                                Text(" (${cardsInCategory[category] ?: 0}) ")
                                Span({
                                    style {
                                        fontSize(14.px)
                                        opacity(.5)
                                    }
                                }) {
                                    val votes = votesInCategory[category] ?: 0
                                    Text("$votes vote${if (votes == 1) "" else "s"}")
                                }
                            }
                        }
                        Tags(
                            tags = tags,
                            selected = tagFilters,
                            marginTop = 0.r,
                            title = if (!isEditingTagCategories) {
                                // todo: translate
                                "Tap to filter"
                            } else {
                                // todo: translate
                                "Tap to set category"
                            },
                            formatCount = { tag ->
                                if (tag == null) {
                                    noTagCount.toString()
                                } else {
                                    (tagCount?.get(tag) ?: 0).toString()
                                }
                            },
                            formatDescription = { tag ->
                                val totalVotes = if (tag == null) {
                                    stagedCards.sumOf { card ->
                                        if (data?.tags?.get(card.id!!)?.notEmpty == null) {
                                            data?.votes?.get(card.id!!) ?: 0
                                        } else {
                                            0
                                        }
                                    }
                                } else {
                                    stagedCards.sumOf { card ->
                                        if (data?.tags?.get(card.id!!)?.contains(tag) == true) {
                                            data?.votes?.get(card.id!!) ?: 0
                                        } else {
                                            0
                                        }
                                    }
                                }

                                if (totalVotes > 0) {
                                    // todo: translate
                                    "$totalVotes ${if (totalVotes == 1) "vote" else "votes"}"
                                } else {
                                    null
                                }
                            },
                            showNoTag = category == null,
                            onClick = { tag, multiselect ->
                                if (isEditingTagCategories) {
                                    if (tag is TagFilter.Tag) {
                                        setTagCategory(tag.tag)
                                    } else {
                                        // No tag
                                    }
                                } else {
                                    if (!multiselect) {
                                        tagFilters = if (tag in tagFilters) {
                                            emptySet()
                                        } else {
                                            setOf(tag)
                                        }
                                    } else {
                                        if (tag in tagFilters) {
                                            tagFilters -= tag
                                        } else {
                                            tagFilters += tag
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        val shownCardsSorted = remember(shownCards, showAll) {
            shownCards.sortedByDescending {
                data?.votes?.get(it.id!!) ?: 0
            }
        }

        PageTreeHeader(
            // todo: translate
            title = "Pages"
        ) {
            IconButton(
                name = "download",
                // todo: translate
                title = "Export",
                small = true,
                styles = {
                    padding(0.r)
                }
            ) {
                scope.launch {
                    dialog(
                        // todo: json
                        title = "Export",
                        cancelButton = null,
                        confirmButton = application.appString { close },
                        content = {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    whiteSpace("pre-wrap")
                                }
                            }) {
                                Text(
                                    shownCardsSorted.joinToString("\n\n") {
                                        // todo: translate
                                        "[${data?.votes?.get(it.id!!) ?: 0}, ${data?.stages?.get(it.id!!) ?: "New"}] ${it.name}\n${
                                            data?.tags?.get(
                                                it.id!!
                                            )?.joinToString(" ") { "($it)" } ?: ""}"
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        val shownCardsShown = shownCardsSorted.let {
            if (showAll) {
                it
            } else {
                it.take(5)
            }
        }

        if (search.isNotBlank() && shownCards.isEmpty()) {
            Empty {
                Text(appString { noCards })
            }
        }

        shownCardsShown.forEach { card ->
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

                        Div({
                            style {
                                if (me != null) {
                                    cursor("pointer")
                                    marginTop(.5.r)
                                }
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                alignItems(AlignItems.Center)
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
                                                    put(card.id!!, result.toIntOrNull() ?: 0)
                                                }
                                            )
                                        )
                                    }
                                }
                            } else {
                                // todo: translate
                                title("Sign in to vote")
                            }
                        }) {
                            if (me != null) {
                                // todo: translate
                                Text("${votes.toLocaleString()} ${if (votes == 1) "vote" else "votes"}")
                            } else {
                                Div({
                                    style {
                                        fontSize(24.px)
                                        fontWeight("bold")
                                    }
                                }) {
                                    Text(votes.toLocaleString())
                                }
                                Text(if (votes == 1) "vote" else "votes")
                            }
                        }
                    }
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            marginRight(1.r)
                            textAlign("center")
                            justifyContent(JustifyContent.Center)
                            alignItems(AlignItems.Center)
                            if (me != null) {
                                cursor("pointer")
                            }
                        }

                        // todo: translate
                        title("Stage")

                        onClick {
                            if (me != null) {
                                setStage(card)
                            }
                        }
                    }) {
                        val stage = data?.stages?.get(card.id!!)
                        if (stage == null) {
                            // todo: translate
                            Span({
                                style {
                                    opacity(.5f)
                                }
                            }) { Text("New") }
                        } else {
                            Span {
                                Text(stage)
                            }
                        }
                    }
                    Div({
                        style {
                            cursor("pointer")
                            flexGrow(1)
                        }

                        if (card.person == me?.id) {
                            // todo: translate
                            title("Edit")
                        } else {
                            // todo: translate
                            title("Open page")
                        }

                        onClick { event ->
                            event.stopPropagation()

                            if (event.ctrlKey) {
                                window.open("/page/${card.id!!}", target = "_blank")
                            } else {
                                if (card.person == me?.id) {
                                    scope.launch {
                                        val result = inputDialog(
                                            title = application.appString { details },
                                            singleLine = false,
                                            confirmButton = application.appString { update },
                                            defaultValue = card.getConversation().message
                                        )

                                        if (result != null) {
                                            saveConversation(card, result)
                                        }
                                    }
                                } else {
                                    router.navigate("/page/${card.id!!}")
                                }
                            }
                        }
                    }) {
                        Div({
                            style {
                                fontWeight("bold")
                                fontSize(18.px)
                            }

                            onClick { event ->
                                event.stopPropagation()

                                if (card.person == me?.id) {
                                    scope.launch {
                                        val result = inputDialog(
                                            title = application.appString { title },
                                            singleLine = false,
                                            confirmButton = application.appString { update },
                                            defaultValue = card.name.orEmpty()
                                        )

                                        if (result != null) {
                                            saveCard(card.id!!, Card(name = result))
                                        }
                                    }
                                }
                            }
                        }) {
                            Text(card.name ?: "")
                        }

                        card.getConversation().message.notBlank?.let {
                            Div({
                                style {
                                    fontSize(16.px)
                                }
                            }) {
                                Text(it)
                            }
                        }

                        val tags = data?.tags?.get(card.id!!) ?: emptyList()

                        Tags(
                            tags = tags,
                            // todo: translate
                            title = if (me != null) "Tap to remove" else "",
                            onClick = { tag, _ ->
                                if (me != null) {
                                    removeTag(card, (tag as? TagFilter.Tag)?.tag ?: "")
                                }
                            }
                        ) {
                            if (me != null) {
                                Button(
                                    {
                                        classes(Styles.outlineButton)

                                        style {
                                            padding(0.r, 1.5.r)
                                            height(2.5.r)
                                        }

                                        // todo: translate
                                        title("Add tag")

                                        onClick {
                                            it.stopPropagation()
                                            addTag(card)
                                        }
                                    }
                                ) {
                                    Icon("new_label") {
                                        marginRight(0.r)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (shownCards.size > 5 && !showAll) {
            Div(
                {
                    style {
                        width(100.percent)
                        marginTop(1.r)
                        display(DisplayStyle.Flex)
                        justifyContent(JustifyContent.Center)
                        alignItems(AlignItems.Center)
                    }
                }
            ) {
                Button(
                    {
                        classes(Styles.outlineButton)
                        onClick {
                            showAll = true
                        }
                    }
                ) {
                    // todo: translate
                    Text("Show ${shownCards.size - 5} more")
                }
            }
        }
    }
}

@Composable
fun Tags(
    tags: List<String>,
    selected: Set<TagFilter> = emptySet(),
    marginTop: CSSSizeValue<*> = 1.r,
    title: String,
    onClick: (tag: TagFilter, multiselect: Boolean) -> Unit,
    formatCount: ((tag: String?) -> String?)? = null,
    formatDescription: ((tag: String?) -> String?)? = null,
    showNoTag: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    Div({
        style {
            marginTop(marginTop)
            display(DisplayStyle.Flex)
            flexWrap(FlexWrap.Wrap)
            gap(.5.r)
        }
    }) {
        tags.forEach { tag ->
            TagButton(
                tag = tag,
                title = title,
                selected = tag in selected.filterIsInstance<TagFilter.Tag>().map { it.tag },
                count = formatCount?.invoke(tag),
                description = formatDescription?.invoke(tag),
                onClick = { multiselect ->
                    onClick(TagFilter.Tag(tag), multiselect)
                }
            )
        }

        if (tags.isNotEmpty() && showNoTag) {
            TagButton(
                // todo: Translate
                tag = "No tag",
                count = formatCount?.invoke(null),
                description = formatDescription?.invoke(null),
                title = title,
                selected = TagFilter.Untagged in selected,
                outline = true,
                onClick = {
                    onClick(TagFilter.Untagged, it)
                }
            )
        }

        content()
    }
}

@Composable
fun TagButton(
    tag: String,
    title: String,
    selected: Boolean,
    description: String? = null,
    count: String? = null,
    outline: Boolean = false,
    onClick: (multiselect: Boolean) -> Unit,
) {
    Button(
        {
            classes(if (outline) Styles.outlineButton else Styles.button)

            if (selected) {
                classes(Styles.buttonSelected)
            }

            style {
                height(2.5.r)
                padding(0.r, 1.5.r)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                alignItems(AlignItems.Start)
                justifyContent(JustifyContent.Center)

                if (!outline) {
                    color(Color.white)
                    backgroundColor(tagColor(tag))
                }
            }

            title(title)

            onClick {
                it.stopPropagation()
                onClick(it.ctrlKey)
            }
        }
    ) {
        Div {
            Text(tag)

            count?.let {
                Span({
                    style {
                        fontWeight("normal")
                        paddingLeft(.25.r)
                    }
                }) { Text(it) }
            }
        }
        if (description?.notBlank != null) {
            Div({
                style {
                    fontSize(12.px)
                    opacity(.667f)
                }
            }) {
                Text(description)
            }
        }
    }
}

@Composable
fun PageTreeHeader(
    title: String,
    actions: @Composable () -> Unit = {},
) {
    Div({
        style {
            fontWeight("bold")
            marginTop(1.r)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Row)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.SpaceBetween)
            width(100.percent)
        }
    }) {
        Text(title)
        actions()
    }
}
