package app.widget

import Styles.card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.compose.rememberDarkMode
import app.widget.space.SpacePathItem
import app.widget.space.SpaceWidgetPath
import app.widget.space.drawCanvas
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.SpaceItem
import json
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import lib.ResizeObserver
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
 * Todos:
 * [ ] Click to add text
 * [ ] ctrl+click to add a page
 * [ ] draw line
 * [ ] draw scribble
 * [ ] draw box
 * [ ] draw circle
 * [ ] insert image [AI, upload]
 * [ ] tools bar
 * [ ] only card owner can edit
 *
 * Later
 * [ ] select card on widget add
 * [ ] multi-select
 * [ ] deal with deleted cards (can be removed from data...)
 * [ ] enter fullscreen/expanded view
 *
 * Done
 * [X] Double-click a page to enter into it inside the widget
 * [X] Save card position
 * [X] Render card photos
 * [X] use widgetId
 * [X] delete item
 */


@Composable
fun SpaceWidget(widgetId: String) {
    var dirty by remember(widgetId) { mutableStateOf<Int?>(null) }
    var data by remember(widgetId) { mutableStateOf<SpaceData?>(null) }
    var cardId by remember(widgetId) { mutableStateOf<String?>(null) }
    var path by remember { mutableStateOf(emptyList<SpacePathItem>()) }

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
    var draggedCircleIndex by remember { mutableStateOf<Int?>(null) }
    var offset by remember { mutableStateOf(0.0 to 0.0) }
    var context by remember { mutableStateOf<CanvasRenderingContext2D?>(null) }

    val items = data?.items ?: listOf()

    var draw by remember {
        mutableStateOf({})
    }

    // todo: kotlin/js bug, if we don't recreate the function, the function uses old values of these variables!
    LaunchedEffect(context, offset, cardsById, items, selectedItem, darkMode) {
        draw = {
            context?.let { drawCanvas(it, offset, cardsById, items, selectedItem, darkMode) }
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
                draw()
            }
        }
    }

    LaunchedEffect(data) {
        draw()
    }

    LaunchedEffect(cards, darkMode) {
        context ?: return@LaunchedEffect

        draw()
    }

    Div(
        attrs = {
            style {
                position(Relative)
                width(100.percent)
                property("aspect-ratio", "2")
                borderRadius(1.r)
            }
        }
    ) {
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
                draw()
            }
        }
        Canvas(
            attrs = {
                style {
                    width(100.percent)
                    height(100.percent)
                    property("aspect-ratio", "2")
                    borderRadius(1.r)
                }

                tabIndex(1)

                onMouseDown { event ->
                    items.forEachIndexed { index, (item, position) ->
                        when (item) {
                            is SpaceContent.Page -> {
                                val (x, y) = position
                                val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                                val mouseX = event.clientX - rect.left - offset.first
                                val mouseY = event.clientY - rect.top - offset.second
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    draggedCircleIndex = index
                                    selectedItem = items[index]
                                }
                            }
                        }
                    }

                    if (draggedCircleIndex == null) {
                        draggingCanvas = offset to (event.clientX.toDouble() to event.clientY.toDouble())
                        selectedItem = null
                    }

                    draw()
                }

                onMouseMove { event ->
                    draggedCircleIndex?.let { index ->
                        val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                        val newX = event.clientX - rect.left - offset.first
                        val newY = event.clientY - rect.top - offset.second
                        data = data!!.copy(
                            items = data!!.items!!.toMutableList().apply {
                                val item = removeAt(index)
                                add(index, item.copy(position = newX to newY))
                            }
                        )
                        dirty = nextInt()
                        draw()
                    }
                    draggingCanvas?.let { (initialOffset, initialMouse) ->
                        offset =
                            (initialOffset.first + (event.clientX - initialMouse.first)) to (initialOffset.second + (event.clientY - initialMouse.second))
                        draw()
                    }
                }

                onMouseUp {
                    draggedCircleIndex = null
                    draggingCanvas = null
                    draw()
                }

                onDoubleClick { event ->
                    items.forEachIndexed { index, (item, position) ->
                        when (item) {
                            is SpaceContent.Page -> {
                                val (x, y) = position
                                val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                                val mouseX = event.clientX - rect.left - offset.first
                                val mouseY = event.clientY - rect.top - offset.second
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
                        }
                    }
                }

                onKeyDown { event ->
                    if (event.key == "Delete") {
                        if (selectedItem != null) {
                            data = data!!.copy(
                                items = data!!.items!!.filter { it.content != selectedItem?.content }
                            )
                            dirty = nextInt()
                            selectedItem = null
                        }
                    } else if (event.key == "Escape") {
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

                ref { canvas ->
                    context = canvas.getContext("2d") as CanvasRenderingContext2D

                    canvas.width = canvas.clientWidth
                    canvas.height = canvas.clientHeight

                    val observer = ResizeObserver { _, _ ->
                        canvas.width = canvas.clientWidth
                        canvas.height = canvas.clientHeight
                        draw()
                    }.apply {
                        observe(canvas)
                    }

                    draw()

                    onDispose {
                        observer.disconnect()
                        context = null
                    }
                }
            }
        )
    }
}
