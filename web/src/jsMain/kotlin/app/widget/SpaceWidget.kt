package app.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import api
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.ailaai.api.newCard
import app.ailaai.api.uploadPhotos
import app.compose.rememberDarkMode
import app.dialog.inputDialog
import app.dialog.rememberChoosePhotoDialog
import app.widget.space.SlideListPanel
import app.widget.space.SlideshowControls
import app.widget.space.SpacePathItem
import app.widget.space.SpaceWidgetPath
import app.widget.space.SpaceWidgetSidePanel
import app.widget.space.SpaceWidgetTool
import app.widget.space.SpaceWidgetToolbar
import app.widget.space.drawCanvas
import app.widget.space.rememberSpaceWidgetInputControl
import application
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceItem
import getImageDimensions
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lib.FullscreenApi
import lib.ResizeObserver
import notBlank
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.events.SyntheticClipboardEvent
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.get
import org.w3c.files.File
import toBytes
import toggleFullscreen
import updateWidget
import web.animations.awaitAnimationFrame
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
    val points: List<Pair<Double, Double>> = emptyList()
)

@Composable
fun SpaceWidget(widgetId: String) {
    val me by application.me.collectAsState()
    var path by remember { mutableStateOf(emptyList<SpacePathItem>()) }

    // Create the control as the single source of truth for state
    val control = rememberSpaceWidgetInputControl()

    // Observe all state at the top level
    val tool = control.collectTool()
    val drawInfo = control.collectDrawInfo()
    val draggedIndex = control.collectDraggedIndex()
    val resizeHandleIndex = control.collectResizeHandleIndex()
    val draggingCanvas = control.collectDraggingCanvas()
    val selectedItem = control.collectSelectedItem()
    val draggedOffset = control.collectDraggedOffset()
    val offset = control.collectOffset()
    val scribblePoints = control.collectScribblePoints()
    val mousePosition = control.collectMousePosition()
    val data = control.collectData()
    val cardId = control.collectCardId()
    val canEdit = control.collectCanEdit()
    val dirty = control.collectDirty()
    val drawFunc = control.collectDrawFunc()

    var cards by remember(cardId) { mutableStateOf(listOf<Card>()) }
    var card by remember(cardId) { mutableStateOf<Card?>(null) }
    var cardsById by remember(cardId) {
        mutableStateOf(emptyMap<String, Card>())
    }

    val darkMode = rememberDarkMode()
    var context by remember { mutableStateOf<CanvasRenderingContext2D?>(null) }
    var canvasRef by remember { mutableStateOf<HTMLCanvasElement?>(null) }
    var spaceRef by remember { mutableStateOf<HTMLElement?>(null) }
    val scope = rememberCoroutineScope()
    val photoDialog = rememberChoosePhotoDialog(showUpload = true)

    var isFullscreen by remember { mutableStateOf(false) }

    // Listen for fullscreen change events
    DisposableEffect(Unit) {
        val fullscreenChangeListener = EventListener {
            isFullscreen = FullscreenApi.isFullscreen
        }

        FullscreenApi.addFullscreenChangeListener(fullscreenChangeListener)

        onDispose {
            FullscreenApi.removeFullscreenChangeListener(fullscreenChangeListener)
        }
    }

    // todo: loading
    // todo: error

    LaunchedEffect(widgetId) {
        api.widget(widgetId) {
            control.updateData(json.decodeFromString(it.data ?: return@widget) ?: return@widget)
            control.updateCardId(control.data?.card)
        }
    }

    LaunchedEffect(cardId, me) {
        api.card(cardId ?: return@LaunchedEffect) {
            card = it
        }

        control.updateCanEdit(card?.person == me?.id)
    }

    LaunchedEffect(dirty) {
        if (dirty != null) {
            delay(2.seconds)
            api.updateWidget(widgetId, Widget(data = json.encodeToString(data))) {
                // todo: notify saved
            }
            control.updateDirty(null)
        }
    }

    LaunchedEffect(cardId) {
        api.cardsCards(cardId ?: return@LaunchedEffect) {
            cards = it
            cardsById = cards.associateBy { it.id!! }
        }
    }

    // Observe slideshow state
    val slideshowMode = control.collectSlideshowMode()
    val slideshowPaused = control.collectSlideshowPaused()
    val itemVisibility = control.collectItemVisibility()
    val currentSlideIndex = control.collectCurrentSlideIndex()

    // Handle slide transitions (auto-advance when active and not paused)
    LaunchedEffect(slideshowMode, currentSlideIndex, slideshowPaused) {
        if (slideshowMode && !slideshowPaused) {
            // Determine duration for current slide (default to controller interval)
            val slideDuration = control.getSlideDuration(currentSlideIndex) ?: control.slideshowInterval
            // Wait for the transition to complete (500ms) and then wait for display time
            delay(500 + slideDuration)
            control.nextSlide()
        }
    }

    LaunchedEffect(
        context,
        offset,
        cardId,
        cards,
        cardsById,
        data,
        selectedItem,
        darkMode,
        drawInfo,
        mousePosition,
        slideshowMode,
        itemVisibility,
        tool
    ) {
        control.updateDrawFunc {
            context?.let {
                drawCanvas(
                    context = it,
                    offset = offset,
                    cardId = cardId,
                    cardsById = cardsById,
                    items = data?.items ?: listOf(),
                    selectedItem = selectedItem,
                    darkMode = darkMode,
                    drawInfo = drawInfo,
                    mousePosition = mousePosition,
                    isItemVisible = control::isItemVisible,
                    itemVisibility = itemVisibility
                )
            }
        }

        control.drawFunc()
    }

    LaunchedEffect(isFullscreen) {
        awaitAnimationFrame()
        drawFunc()
    }

    // Auto-insert newly added cards
    LaunchedEffect(cards, data == null) {
        data ?: return@LaunchedEffect

        cards.filter { card ->
            (data.items ?: emptyList())
                .map { it.content }
                .filterIsInstance<SpaceContent.Page>()
                .none { it.id == card.id }
        }.onEachIndexed { index, card ->
            control.updateData(
                data.copy(
                    items = (data.items ?: emptyList()) + SpaceItem(
                        content = SpaceContent.Page(card.id!!),
                        position = (200.0 + 200.0 * index) to 200.0
                    )
                )
            )
        }.also {
            if (it.isNotEmpty()) {
                control.updateDirty(nextInt())
                drawFunc()
            }
        }
    }

    cardId ?: return

    Div(
        attrs = {
            classes(WidgetStyles.spaceContainer)

            ref {
                spaceRef = it

                onDispose {
                    spaceRef = null
                }
            }
        }
    ) {
        Canvas(
            attrs = {
                classes(WidgetStyles.space)

                tabIndex(1)

                onPaste { clipboardEvent: SyntheticClipboardEvent ->
                    if (!canEdit) return@onPaste

                    // Get clipboard data
                    val clipboardData = clipboardEvent.clipboardData ?: return@onPaste

                    // Check for images
                    val items = clipboardData.items
                    val photos = mutableListOf<File>()

                    for (i in 0 until items.length) {
                        val item = items[i] ?: continue
                        val type = item.type

                        if (type.startsWith("image/")) {
                            val file = item.getAsFile() ?: continue
                            photos.add(file)
                        }
                    }

                    if (photos.isNotEmpty()) {
                        // Handle pasted image
                        clipboardEvent.preventDefault() // Prevent default paste behavior

                        // Process all photos
                        photos.forEach { photo ->
                            scope.launch {
                                try {
                                    // Get image dimensions
                                    val dimensions = photo.getImageDimensions()

                                    // Convert File to ByteArray
                                    val photoBytes = photo.toBytes()

                                    // Upload the image
                                    api.uploadPhotos(
                                        photos = listOf(photoBytes),
                                        onSuccess = { response ->
                                            val photoUrl = response.urls.firstOrNull() ?: return@uploadPhotos

                                            // Add the photo to the canvas using the control
                                            val canvas = context?.canvas
                                            val canvasWidth = canvas?.width?.toDouble() ?: 800.0
                                            val canvasHeight = canvas?.height?.toDouble() ?: 600.0

                                            control.addPhotoAtCenter(
                                                photoUrl = photoUrl,
                                                width = dimensions.width,
                                                height = dimensions.height,
                                                canvasWidth = canvasWidth,
                                                canvasHeight = canvasHeight
                                            )
                                        }
                                    )
                                } catch (e: Exception) {
                                    console.error("Error processing pasted image", e)
                                }
                            }
                        }
                    }
                }

                onMouseEnter {
                    canvasRef?.focus()
                }

                onTouchStart { event: SyntheticTouchEvent ->
                    event.preventDefault()
                    event.touches[0]?.let {
                        control.onMouseDown(
                            (event.target as HTMLCanvasElement).getBoundingClientRect(),
                            it.clientX.toDouble(),
                            it.clientY.toDouble()
                        )
                    }
                }

                onTouchMove { event: SyntheticTouchEvent ->
                    event.preventDefault()
                    event.touches[0]?.let {
                        control.onMouseMove(
                            (event.target as HTMLCanvasElement).getBoundingClientRect(),
                            it.clientX.toDouble(),
                            it.clientY.toDouble()
                        )
                    }
                }

                onTouchEnd { event: SyntheticTouchEvent ->
                    event.preventDefault()
                    control.onMouseUp()
                }

                onMouseLeave {
                    control.updateMousePosition(null)
                    drawFunc()
                }

                onMouseDown { event: SyntheticMouseEvent ->
                    event.preventDefault()
                    control.onMouseDown(
                        (event.target as HTMLCanvasElement).getBoundingClientRect(),
                        event.clientX.toDouble(),
                        event.clientY.toDouble()
                    )
                }

                onMouseMove { event: SyntheticMouseEvent ->
                    event.preventDefault()
                    control.updateShiftDown(event.shiftKey)
                    control.onMouseMove(
                        (event.target as HTMLCanvasElement).getBoundingClientRect(),
                        event.clientX.toDouble(),
                        event.clientY.toDouble()
                    )
                }

                onMouseUp { event: SyntheticMouseEvent ->
                    event.preventDefault()
                    control.onMouseUp()
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
                                        title = application.appString { text },
                                        singleLine = false
                                    )

                                    if (!text.isNullOrBlank()) {
                                        control.addText(mouseX, mouseY, text)
                                    }
                                }
                            }

                            SpaceWidgetTool.Photo -> {
                                scope.launch {
                                    photoDialog.launch(
                                        multiple = false,
                                        onPhoto = { photoUrl: String, width: Int?, height: Int? ->
                                            control.addPhoto(mouseX, mouseY, photoUrl, width, height)
                                        }
                                    )
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
                                    control.addCard(mouseX, mouseY, newCard.id!!)

                                    api.cardsCards(cardId) {
                                        cards = it
                                        cardsById = cards.associateBy { it.id!! }
                                    }
                                }
                            }
                        }
                    }
                }

                onDoubleClick { event ->
                    val items = data?.items ?: return@onDoubleClick

                    items.forEachIndexed { index, (item, position) ->
                        val (x, y) = position
                        val rect = (event.target as HTMLCanvasElement).getBoundingClientRect()
                        val mouseX = event.clientX - rect.left - offset.first
                        val mouseY = event.clientY - rect.top - offset.second

                        when (item) {
                            is SpaceContent.Page -> {
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    // Enter into page
                                    cardId.let { currentCardId ->
                                        val pathItem = SpacePathItem(
                                            id = currentCardId,
                                            card = card ?: return@forEachIndexed,
                                            offset = offset,
                                            selectedItem = selectedItem
                                        )
                                        path += pathItem

                                        // Update cardId in control
                                        control.navigateToCard(item.id, pathItem)
                                    }
                                }
                            }

                            is SpaceContent.Text -> {
                                if (canEdit) {
                                    if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                        scope.launch {
                                            val text = inputDialog(
                                                title = application.appString { text },
                                                defaultValue = item.text.orEmpty(),
                                                singleLine = false
                                            )

                                            if (!text.isNullOrBlank()) {
                                                control.updateText(index, text)
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
                    // Slideshow navigation via keyboard
                    if (slideshowMode) {
                        when (event.key) {
                            "ArrowLeft" -> {
                                event.preventDefault()
                                control.previousSlide()
                            }
                            "ArrowRight" -> {
                                event.preventDefault()
                                control.nextSlide()
                            }
                            " " -> {
                                event.preventDefault()
                                if (slideshowPaused) control.resumeSlideshow() else control.pauseSlideshow()
                            }
                            else -> {}
                        }
                        return@onKeyDown
                    }
                    // Delete or escape handling
                    if (event.key == "Delete") {
                        if (canEdit) {
                            if (selectedItem != null) {
                                control.deleteSelectedItem()
                            }
                        }
                    } else if (event.key == "Escape") {
                        if (drawInfo != null) {
                            control.updateDrawInfo(null)
                            drawFunc()
                        } else {
                            // Exit from page
                            if (path.isNotEmpty()) {
                                val pathItem = path.last()
                                control.navigateBack(pathItem)
                                path = path.dropLast(1)
                            }
                        }
                    }
                }

                ref { canvas ->
                    canvasRef = canvas
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
                control.updateCardId(item.id)
                control.updateOffset(item.offset)
                control.updateSelectedItem(item.selectedItem)
                drawFunc()
            }
        }

        if (canEdit) {
            SpaceWidgetToolbar(
                tool = tool,
                onTool = {
                    control.updateTool(it)
                }
            )

            // Show side panel when an item is selected
            SpaceWidgetSidePanel(
                onFullscreen = {
                    spaceRef.toggleFullscreen()
                },
                isFullscreen = isFullscreen,
                showContentTools = selectedItem != null,
                onSendToBack = {
                    // Move the selected item to the beginning of the list (back)
                    data?.items?.let { items ->
                        val selectedItemIndex = items.indexOfFirst { item -> item.content == selectedItem?.content }
                        if (selectedItemIndex > 0) {
                            val newItems = items.toMutableList()
                            val item = newItems.removeAt(selectedItemIndex)
                            newItems.add(0, item)
                            val newData = data.copy(items = newItems)
                            control.updateData(newData)
                            control.updateDirty(nextInt())
                        }
                    }
                },
                onBringToFront = {
                    // Move the selected item to the end of the list (front)
                    data?.items?.let { items ->
                        val selectedItemIndex = items.indexOfFirst { item -> item.content == selectedItem?.content }
                        if (selectedItemIndex >= 0 && selectedItemIndex < items.size - 1) {
                            val newItems = items.toMutableList()
                            val item = newItems.removeAt(selectedItemIndex)
                            newItems.add(item)
                            val newData = data.copy(items = newItems)
                            control.updateData(newData)
                            control.updateDirty(nextInt())
                        }
                    }
                },
                onToggleSlideshow = {
                    if (slideshowMode) {
                        control.stopSlideshow()
                    } else {
                        control.startSlideshow()
                    }
                },
                isSlideshowActive = slideshowMode,
                onDuplicate = {
                    control.duplicateSelectedItem()
                },
                onDelete = {
                    control.deleteSelectedItem()
                }
            )
        }

        // Show slideshow controls when in slideshow mode
        if (slideshowMode) {
            val slides = control.collectSlides()
            SlideshowControls(
                currentSlide = currentSlideIndex,
                totalSlides = slides.size,
                isPaused = slideshowPaused,
                onPrevious = { _ -> control.previousSlide() },
                onPause = { _ ->
                    if (slideshowPaused) control.resumeSlideshow() else control.pauseSlideshow()
                },
                onNext = { _ -> control.nextSlide() },
                onExit = { _ -> control.stopSlideshow() },
                onCreate = { _ ->
                    // Auto-generate slide name
                    val newTitle = "Slide ${slides.size + 1}"
                    control.createSlide(newTitle)
                },
                onRename = { _ ->
                    scope.launch {
                        val current = slides.getOrNull(currentSlideIndex)
                        val currentTitle = (current?.content as? SpaceContent.Slide)?.title.orEmpty()
                        val newTitle = inputDialog(
                            title = "Rename Slide",
                            defaultValue = currentTitle,
                            singleLine = true
                        )
                        if (!newTitle.isNullOrBlank()) {
                            control.renameSlide(currentSlideIndex, newTitle)
                        }
                    }
                },
                onDuration = { _ ->
                    scope.launch {
                        // Prompt for duration in seconds
                        val currentDuration = control.getSlideDuration(currentSlideIndex) ?: 5000L
                        val input = inputDialog(
                            title = "Slide Duration (seconds)",
                            defaultValue = (currentDuration / 1000).toString(),
                            singleLine = true,
                            type = InputType.Number
                        )
                        val seconds = input?.toLongOrNull()
                        seconds?.let { sec ->
                            control.updateSlideDuration(currentSlideIndex, sec * 1000)
                        }
                    }
                },
                onDelete = { _ -> control.deleteSlide(currentSlideIndex) }
            )
        }

        // Always show slide list panel when in slideshow mode
        if (slideshowMode) {
            val slides = control.collectSlides()
            SlideListPanel(
                slides = slides,
                currentSlideIndex = currentSlideIndex,
                onSelectSlide = { index, _ ->
                    control.showSlide(index)
                    control.updateCurrentSlideIndex(index)
                }
            )
        }
    }
}
