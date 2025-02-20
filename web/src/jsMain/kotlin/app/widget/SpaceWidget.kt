package app.widget

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import api
import app.ailaai.api.cardsCards
import app.compose.rememberDarkMode
import baseUrl
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.SpaceItem
import json
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import lib.ResizeObserver
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Canvas
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.MIDDLE
import r
import updateWidget
import widget
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.seconds

/**
 * Todos:
 * [ ] Double-click a page to enter into it inside the widget
 * [ ] Save card position
 * [ ] Render card photos
 * [ ] Click to add text
 * [ ] use widgetId
 * [ ] draw line
 * [ ] tools bar
 * [ ] delete item
 * [ ] only card owner can edit
 * [ ] enter fullscreen/expanded view
 * [ ] deal with deleted cards
 * [ ] multi-select
 * [ ] add text
 */

private fun drawCanvas(
    context: CanvasRenderingContext2D,
    offset: Pair<Double, Double>,
    cardsById: Map<String, Card>,
    items: List<SpaceItem>,
    selectedItem: SpaceItem?,
    darkMode: Boolean,
) {
    with(context) {
        clearRect(
            x = 0.0,
            y = 0.0,
            w = canvas.width.toDouble(),
            h = canvas.height.toDouble()
        )

        // Apply the offset transformation
        save()
        translate(offset.first, offset.second)

        // Draw the pages
        items.forEachIndexed { index, (item, position) ->
            when (item) {
                is SpaceContent.Page -> {
                    val card = cardsById[item.id] ?: return@forEachIndexed
                    val (x, y) = position

                    save()

                    if (card.photo != null) {
                        val image = js("new Image()") as HTMLImageElement
                        image.src = "$baseUrl${card.photo!!}"
                        beginPath()
                        arc(x, y, 24.0, 0.0, 2 * PI)
                        clip()
                        val imageAspect = image.width.toDouble() / image.height.toDouble()
                        val circleDiameter = 48.0
                        val circleRadius = circleDiameter / 2

                        if (imageAspect > 1) {
                            val scaledHeight = circleDiameter
                            val scaledWidth = circleDiameter * imageAspect
                            drawImage(
                                image = image,
                                dx = x - circleRadius - (scaledWidth - circleDiameter) / 2,
                                dy = y - circleRadius,
                                dw = scaledWidth,
                                dh = scaledHeight
                            )
                        } else {
                            val scaledWidth = circleDiameter
                            val scaledHeight = circleDiameter / imageAspect
                            drawImage(
                                image = image,
                                dx = x - circleRadius,
                                dy = y - circleRadius - (scaledHeight - circleDiameter) / 2,
                                dw = scaledWidth,
                                dh = scaledHeight
                            )
                        }
                    }

                    beginPath()
                    arc(x, y, 24.0, 0.0, 2 * PI)
                    lineWidth = if (selectedItem?.content == item) 3.0 else 1.0
                    strokeStyle = if (darkMode) Color.gray else Styles.colors.primary
                    stroke()

                    restore()

                    // Draw the page name
                    fillStyle = if (darkMode) Color.white else Color.black
                    font = "18px ${font.split(" ").last()}"
                    textAlign = CanvasTextAlign.CENTER
                    textBaseline = CanvasTextBaseline.MIDDLE

                    fillText(card.name ?: "New page", x, y - 24 - 18)
                }
            }
        }

        restore()
    }
}

@Composable
fun SpaceWidget(widgetId: String) {
    var cardId by remember(widgetId) { mutableStateOf<String?>(null) }
    var data by remember(widgetId) { mutableStateOf<SpaceData?>(null) }

    LaunchedEffect(widgetId) {
        api.widget(widgetId) {
            data = json.decodeFromString(it.data ?: return@widget) ?: return@widget
            cardId = data?.card
        }
    }

    // todo: loading
    cardId ?: return

    var dirty by remember(cardId) { mutableStateOf<Int?>(null) }
    var cards by remember(cardId) { mutableStateOf(listOf<Card>()) }
    var cardsById by remember(cardId) {
        mutableStateOf(emptyMap<String, Card>())
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
        // Todo load all to max depth
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

    // Handle mouse move event to update circle position
    fun handleMouseMove(event: SyntheticMouseEvent) {
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

    LaunchedEffect(cards, darkMode) {
        context ?: return@LaunchedEffect
        draw()
    }

    // Render the canvas and attach event listeners
    Canvas(
        attrs = {
            style {
                width(100.percent)
                height(32.r)
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
                handleMouseMove(event)
            }

            onMouseUp {
                draggedCircleIndex = null
                draggingCanvas = null
                draw()
            }

            onKeyDown { event ->
                if (event.key == "Delete" && selectedItem != null) {
                    data = data!!.copy(
                        items = data!!.items!!.filter { it.content != selectedItem?.content }
                    )
                    dirty = nextInt()
                    selectedItem = null
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
