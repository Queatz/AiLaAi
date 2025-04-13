package app.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.newCard
import app.compose.rememberDarkMode
import app.dialog.inputDialog
import app.widget.space.SpacePathItem
import app.widget.space.SpaceWidgetPath
import app.widget.space.SpaceWidgetTool
import app.widget.space.SpaceWidgetToolbar
import app.widget.space.drawCanvas
import application
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.SpaceItem
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import lib.ResizeObserver
import notBlank
import org.jetbrains.compose.web.css.Position.Companion.Relative
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import r
import updateWidget
import widget
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.seconds

/**
 * Incoming:
 *
 * [ ]
 *
 * Todos:
 * [ ] draw scribble
 * [ ] draw circle
 * [ ] insert image [AI, upload]
 * [ ] only card owner can edit
 * [ ] ctrl+z ctrl+shift+z
 *
 * Later:
 * [ ] select card on widget add
 * [ ] multi-select
 * [ ] drag select
 * [ ] deal with deleted cards (can be removed from data...)
 * [ ] enter fullscreen/expanded view
 * [ ] Space: Snap to grid
 * [ ] Space: Show grid
 *
 * Done
 * [X] tools bar
 * [X] Click to add text
 * [X] ctrl+click to add a page
 * [X] draw box
 * [X] draw line
 * [X] Double-click a page to enter into it inside the widget
 * [X] Save card position
 * [X] Render card photos
 * [X] use widgetId
 * [X] delete item
 */

data class DrawInfo(
    val tool: SpaceWidgetTool,
    val from: Pair<Double, Double>? = null,
    val to: Pair<Double, Double>? = null,
)

@Composable
fun SpaceWidget(widgetId: String) {
    val me by application.me.collectAsState()
    var dirty by remember(widgetId) { mutableStateOf<Int?>(null) }
    var data by remember(widgetId) { mutableStateOf<SpaceData?>(null) }
    var cardId by remember(widgetId) { mutableStateOf<String?>(null) }
    var path by remember { mutableStateOf(emptyList<SpacePathItem>()) }
    var tool by remember { mutableStateOf<SpaceWidgetTool>(SpaceWidgetTool.Default) }

    LaunchedEffect(widgetId) {
        api.widget(widgetId) {
            data = json.decodeFromString(it.data ?: return@widget) ?: return@widget
            cardId = data?.card
        }
    }

    // todo: loading
    // todo: error

    cardId ?: return

    var cards by remember(cardId) { mutableStateOf(listOf<Card>()) }
    var card by remember(cardId) { mutableStateOf<Card?>(null) }
    var cardsById by remember(cardId) {
        mutableStateOf(emptyMap<String, Card>())
    }

    LaunchedEffect(cardId) {
        cardId?.let { cardId ->
            api.card(cardId) {
                card = it
            }
        }
    }

    val canEdit = card?.person == me?.id

    LaunchedEffect(dirty) {
        if (dirty != null) {
            delay(2.seconds)
            api.updateWidget(widgetId, Widget(data = json.encodeToString(data))) {
                // todo: notify saved
            }
            dirty = null
        }
    }

    LaunchedEffect(cardId) {
        api.cardsCards(cardId!!) {
            cards = it
            cardsById = cards.associateBy { it.id!! }
        }
    }

    val darkMode = rememberDarkMode()
    var draggingCanvas: Pair<Pair<Double, Double>, Pair<Double, Double>>? by remember { mutableStateOf(null) }
    var selectedItem by remember { mutableStateOf<SpaceItem?>(null) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffset by remember { mutableStateOf(0.0 to 0.0) }
    var offset by remember { mutableStateOf(0.0 to 0.0) }
    var drawInfo by remember { mutableStateOf<DrawInfo?>(null) }
    var context by remember { mutableStateOf<CanvasRenderingContext2D?>(null) }
    val scope = rememberCoroutineScope()

    val items = data?.items ?: listOf()

    var drawFunc by remember {
        mutableStateOf({})
    }

    // todo: kotlin/js bug, if we don't recreate the function, the function uses old values of these variables!
    LaunchedEffect(
        context,
        offset,
        cardsById,
        items,
        selectedItem,
        darkMode,
        drawInfo
    ) {
        drawFunc = {
            context?.let {
                drawCanvas(
                    context = it,
                    offset = offset,
                    cardId = cardId,
                    cardsById = cardsById,
                    items = items,
                    selectedItem = selectedItem,
                    darkMode = darkMode,
                    drawInfo = drawInfo,
                )
            }
        }
    }

    // Auto-insert newly added cards
    LaunchedEffect(cards, data == null) {
        data ?: return@LaunchedEffect

        cards.filter { card ->
            (data!!.items ?: emptyList())
                .map { it.content }
                .filterIsInstance<SpaceContent.Page>()
                .none { it.id == card.id }
        }.onEachIndexed { index, card ->
            data = data!!.copy(
                items = (data!!.items ?: emptyList()) + SpaceItem(
                    content = SpaceContent.Page(card.id!!),
                    position = (200.0 + 200.0 * index) to 200.0
                )
            )
        }.also {
            if (it.isNotEmpty()) {
                dirty = nextInt()
                drawFunc()
            }
        }
    }

    LaunchedEffect(data) {
        drawFunc()
    }

    LaunchedEffect(cards, darkMode) {
        context ?: return@LaunchedEffect

        drawFunc()
    }

    Div(
        attrs = {
            classes(WidgetStyles.spaceContainer)
        }
    ) {
        Canvas(
            attrs = {
                classes(WidgetStyles.space)

                style {
                    width(100.percent)
                    height(100.percent)
                    borderRadius(1.r)
                }

                tabIndex(1)

                onMouseDown { event ->
                    when (tool) {
                        SpaceWidgetTool.Default -> {
                            var foundItem = false
                            items.forEachIndexed { index, (item, position) ->
                                val (x, y) = position
                                val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                                val mouseX = event.clientX - rect.left - offset.first
                                val mouseY = event.clientY - rect.top - offset.second

                                if ((sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) || when (item) {
                                        is SpaceContent.Line -> sqrt(
                                            (mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(
                                                2
                                            )
                                        ) <= 24.0

                                        is SpaceContent.Box -> sqrt(
                                            (mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(
                                                2
                                            )
                                        ) <= 24.0 || sqrt(
                                            (mouseX - item.to.first).pow(2) + (mouseY - y).pow(
                                                2
                                            )
                                        ) <= 24.0 || sqrt(
                                            (mouseX - x).pow(2) + (mouseY - item.to.second).pow(
                                                2
                                            )
                                        ) <= 24.0

                                        else -> false
                                    }
                                ) {
                                    foundItem = true
                                    if (canEdit) {
                                        draggedIndex = index
                                        draggedOffset = (mouseX - x) to (mouseY - y)
                                    }
                                    selectedItem = items[index]
                                }
                            }

                            if (!foundItem) {
                                draggingCanvas = offset to (event.clientX.toDouble() to event.clientY.toDouble())
                                selectedItem = null
                            }

                            drawFunc()
                        }

                        SpaceWidgetTool.Text -> Unit

                        SpaceWidgetTool.Line,
                        SpaceWidgetTool.Box,
                        SpaceWidgetTool.Circle,
                            -> {
                            val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                            val mouseX = event.clientX - rect.left - offset.first
                            val mouseY = event.clientY - rect.top - offset.second
                            drawInfo = DrawInfo(tool = tool, from = mouseX to mouseY)
                        }
                    }
                }

                onMouseMove { event ->
                    when (tool) {
                        SpaceWidgetTool.Default -> {
                            if (canEdit) {
                                draggedIndex?.let { index ->
                                    val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                                    val newX = event.clientX - rect.left - offset.first - draggedOffset.first
                                    val newY = event.clientY - rect.top - offset.second - draggedOffset.second
                                    data = data!!.copy(
                                        items = data!!.items!!.toMutableList().apply {
                                            val item = removeAt(index)
                                            add(index, item.move(newX to newY))
                                        }
                                    )
                                    dirty = nextInt()
                                    drawFunc()
                                }
                            }

                            draggingCanvas?.let { (initialOffset, initialMouse) ->
                                offset =
                                    (initialOffset.first + (event.clientX - initialMouse.first)) to (initialOffset.second + (event.clientY - initialMouse.second))
                                drawFunc()
                            }
                        }

                        SpaceWidgetTool.Text -> Unit

                        SpaceWidgetTool.Line,
                        SpaceWidgetTool.Box,
                        SpaceWidgetTool.Circle,
                            -> {
                            drawInfo?.let {
                                val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                                val newX = event.clientX - rect.left - offset.first
                                val newY = event.clientY - rect.top - offset.second
                                drawInfo = it.copy(to = newX to newY)
                                drawFunc()
                            }
                        }
                    }

                }

                onMouseUp {
                    when (tool) {
                        SpaceWidgetTool.Default -> {
                            draggedIndex = null
                            draggingCanvas = null
                            drawFunc()
                        }

                        SpaceWidgetTool.Text -> Unit

                        SpaceWidgetTool.Line,
                        SpaceWidgetTool.Box,
                        SpaceWidgetTool.Circle,
                            -> {
                            drawInfo?.from?.let { from ->
                                drawInfo?.to?.let { to ->
                                    data = data!!.copy(
                                        items = items + SpaceItem(
                                            content = when (drawInfo?.tool) {
                                                SpaceWidgetTool.Box -> {
                                                    SpaceContent.Box(
                                                        page = cardId,
                                                        to = to
                                                    )
                                                }

                                                SpaceWidgetTool.Circle -> {
                                                    SpaceContent.Circle(
                                                        page = cardId,
                                                        to = to
                                                    )
                                                }

                                                else -> {
                                                    SpaceContent.Line(
                                                        page = cardId,
                                                        to = to
                                                    )
                                                }
                                            },
                                            position = from
                                        )
                                    )
                                    dirty = nextInt()
                                }
                            }

                            drawInfo = null
                            drawFunc()
                        }
                    }
                }

                onClick { event ->
                    if (!canEdit) return@onClick

                    val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                    val mouseX = event.clientX - rect.left - offset.first
                    val mouseY = event.clientY - rect.top - offset.second

                    if (!event.ctrlKey) {
                        when (tool) {
                            SpaceWidgetTool.Text -> {
                                scope.launch {
                                    val text = inputDialog(
                                        // todo: translate
                                        title = "Text",
                                        singleLine = false
                                    )

                                    if (!text.isNullOrBlank()) {
                                        data = data!!.copy(
                                            items = data!!.items!!.toMutableList().apply {
                                                add(SpaceItem(SpaceContent.Text(page = cardId, text), mouseX to mouseY))
                                            }
                                        )
                                        dirty = nextInt()
                                    }
                                }
                            }

                            else -> Unit
                        }
                    } else {
                        scope.launch {
                            val newCardName = inputDialog(title = application.appString { newCard })?.notBlank

                            if (newCardName != null) {
                                api.newCard(
                                    card = Card(
                                        name = newCardName,
                                        parent = cardId,
                                        active = card?.active == true,
                                    )
                                ) { newCard ->
                                    data = data!!.copy(
                                        items = data!!.items!!.toMutableList().apply {
                                            add(SpaceItem(SpaceContent.Page(newCard.id!!), mouseX to mouseY))
                                        }
                                    )
                                    dirty = nextInt()
                                    api.cardsCards(cardId ?: return@newCard) {
                                        cards = it
                                        cardsById = cards.associateBy { it.id!! }
                                    }
                                }
                            }
                        }
                    }
                }

                onDoubleClick { event ->
                    items.forEachIndexed { index, (item, position) ->
                        val (x, y) = position
                        val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                        val mouseX = event.clientX - rect.left - offset.first
                        val mouseY = event.clientY - rect.top - offset.second

                        when (item) {
                            is SpaceContent.Page -> {
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    // Enter into page
                                    cardId?.let { cardId ->
                                        path += SpacePathItem(
                                            id = cardId,
                                            card = card ?: return@forEachIndexed,
                                            offset = offset,
                                            selectedItem = selectedItem
                                        )
                                    }
                                    cardId = item.id
                                    offset = 0.0 to 0.0
                                }
                            }

                            is SpaceContent.Text -> {
                                if (canEdit) {
                                    if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                        scope.launch {
                                            val text = inputDialog(
                                                // todo: translate
                                                title = "Text",
                                                defaultValue = item.text.orEmpty(),
                                                singleLine = false
                                            )

                                            if (!text.isNullOrBlank()) {
                                                data = data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index,
                                                            SpaceItem(
                                                                SpaceContent.Text(page = item.page, text),
                                                                position
                                                            )
                                                        )
                                                    }
                                                )
                                                dirty = nextInt()
                                            }
                                        }
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                }

                onKeyDown { event ->
                    if (event.key == "Delete") {
                        if (canEdit) {
                            if (selectedItem != null) {
                                data = data!!.copy(
                                    items = data!!.items!!.filter { it.content != selectedItem?.content }
                                )
                                dirty = nextInt()
                                selectedItem = null
                            }
                        }
                    } else if (event.key == "Escape") {
                        if (drawInfo != null) {
                            drawInfo = null
                            drawFunc()
                        } else {
                            // Exit from page
                            if (path.isNotEmpty()) {
                                val item = path.last()
                                cardId = item.id
                                offset = item.offset
                                selectedItem = item.selectedItem
                                path = path.dropLast(1)
                            }
                        }
                    }
                }

                ref { canvas ->
                    context = canvas.getContext("2d") as CanvasRenderingContext2D

                    canvas.width = canvas.clientWidth
                    canvas.height = canvas.clientHeight

                    val observer = ResizeObserver { _, _ ->
                        canvas.width = canvas.clientWidth
                        canvas.height = canvas.clientHeight
                        drawFunc()
                    }.apply {
                        observe(canvas)
                    }

                    drawFunc()

                    onDispose {
                        observer.disconnect()
                        context = null
                    }
                }
            }
        )

        card?.let { card ->
            SpaceWidgetPath(
                path = path,
                currentPath = SpacePathItem(
                    id = cardId!!,
                    card = card,
                )
            ) { index ->
                val item = path[index]
                path = path.take(index)
                cardId = item.id
                offset = item.offset
                selectedItem = item.selectedItem
                drawFunc()
            }
        }

        if (canEdit) {
            SpaceWidgetToolbar(
                tool = tool,
                onTool = {
                    tool = it
                }
            )
        }
    }
}

private fun SpaceItem.move(position: Pair<Double, Double>): SpaceItem {
    val offset = (position.first - this.position.first) to (position.second - this.position.second)

    return when (val content = content) {
        is SpaceContent.Box -> {
            copy(
                content = SpaceContent.Box(
                    page = content.page,
                    to = (content.to.first + offset.first) to (content.to.second + offset.second)
                )
            )
        }

        is SpaceContent.Line -> {
            copy(
                content = SpaceContent.Line(
                    page = content.page,
                    to = (content.to.first + offset.first) to (content.to.second + offset.second)
                )
            )
        }

        is SpaceContent.Circle -> {
            copy(
                content = SpaceContent.Circle(
                    page = content.page,
                    to = (content.to.first + offset.first) to (content.to.second + offset.second)
                )
            )
        }

        else -> this@move
    }.copy(position = position)
}
