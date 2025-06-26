package app.widget.space

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.widget.DrawInfo
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.SpaceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.DOMRect
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextInt

// Class to store view state before entering slideshow mode
data class ViewState(
    val offset: Pair<Double, Double>,
    val selectedItem: SpaceItem?
)

class SpaceWidgetInputControl {
    private val _tool = MutableStateFlow<SpaceWidgetTool>(SpaceWidgetTool.Default)
    private val _drawInfo = MutableStateFlow<DrawInfo?>(null)
    private val _draggedIndex = MutableStateFlow<Int?>(null)
    private val _resizeHandleIndex = MutableStateFlow<String?>(null)
    private val _draggingCanvas = MutableStateFlow<Pair<Pair<Double, Double>, Pair<Double, Double>>?>(null)
    private val _selectedItem = MutableStateFlow<SpaceItem?>(null)
    private val _draggedOffset = MutableStateFlow(0.0 to 0.0)
    private val _offset = MutableStateFlow(0.0 to 0.0)
    private val _scribblePoints = MutableStateFlow(mutableListOf<Pair<Double, Double>>())
    private val _mousePosition = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _data = MutableStateFlow<SpaceData?>(null)
    // Stable IDs for items, aligned by index in data.items
    private val _stableIds = MutableStateFlow<List<String>>(emptyList())
    private var nextStableId = 0
    private val _cardId = MutableStateFlow<String?>(null)
    private val _canEdit = MutableStateFlow(false)
    private val _dirty = MutableStateFlow<Int?>(null)
    private val _drawFunc = MutableStateFlow {}

    // Slideshow state
    private val _slideshowMode = MutableStateFlow(false)
    private val _currentSlideIndex = MutableStateFlow(-1)
    private val _slides = MutableStateFlow<List<SpaceItem>>(emptyList())
    // Fallback slide interval when slide.duration is unavailable
    private val defaultSlideInterval = 5000L
    private val _slideshowPaused = MutableStateFlow(false)
    private val _savedViewState = MutableStateFlow<ViewState?>(null)
    private val _itemVisibility = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    // Tracks shift key state for aspect-ratio constrained resizing
    private val _shiftDown = MutableStateFlow(false)

    // Accessor for shift key state
    val shiftDown: Boolean
        get() = _shiftDown.value

    /** Updates the shift key state (for aspect-ratio resizing) */
    fun updateShiftDown(value: Boolean) { _shiftDown.value = value }

    // Public accessors for slideshow state
    val slideshowMode: Boolean
        get() = _slideshowMode.value

    val currentSlideIndex: Int
        get() = _currentSlideIndex.value

    val slides: List<SpaceItem>
        get() = _slides.value
    /**
     * Get durations map for slides by stable ID
     */

    val slideshowInterval: Long
        get() = defaultSlideInterval

    val slideshowPaused: Boolean
        get() = _slideshowPaused.value

    val itemVisibility: Map<String, Boolean>
        get() = _itemVisibility.value

    // Public properties that access the .value of the MutableStateFlow
    var tool: SpaceWidgetTool
        get() = _tool.value
        private set(value) { _tool.value = value }

    var drawInfo: DrawInfo?
        get() = _drawInfo.value
        private set(value) { _drawInfo.value = value }

    var draggedIndex: Int?
        get() = _draggedIndex.value
        private set(value) { _draggedIndex.value = value }

    var resizeHandleIndex: String?
        get() = _resizeHandleIndex.value
        private set(value) { _resizeHandleIndex.value = value }

    var draggingCanvas: Pair<Pair<Double, Double>, Pair<Double, Double>>?
        get() = _draggingCanvas.value
        private set(value) { _draggingCanvas.value = value }

    var selectedItem: SpaceItem?
        get() = _selectedItem.value
        private set(value) { _selectedItem.value = value }

    var draggedOffset: Pair<Double, Double>
        get() = _draggedOffset.value
        private set(value) { _draggedOffset.value = value }

    var offset: Pair<Double, Double>
        get() = _offset.value
        private set(value) { _offset.value = value }

    var scribblePoints: MutableList<Pair<Double, Double>>
        get() = _scribblePoints.value
        private set(value) { _scribblePoints.value = value }

    var mousePosition: Pair<Double, Double>?
        get() = _mousePosition.value
        private set(value) { _mousePosition.value = value }

    var data: SpaceData?
        get() = _data.value
        private set(value) { _data.value = value }

    var cardId: String?
        get() = _cardId.value
        private set(value) { _cardId.value = value }

    var canEdit: Boolean
        get() = _canEdit.value
        private set(value) { _canEdit.value = value }

    var dirty: Int?
        get() = _dirty.value
        private set(value) { _dirty.value = value }

    var drawFunc: () -> Unit
        get() = _drawFunc.value
        private set(value) { _drawFunc.value = value }

    // Generate a new stable ID
    private fun generateStableId(): String = (nextStableId++).toString()
    // Methods to update state from SpaceWidget
    fun updateTool(value: SpaceWidgetTool) { _tool.value = value }
    fun updateDrawInfo(value: DrawInfo?) { _drawInfo.value = value }
    fun updateDraggedIndex(value: Int?) { _draggedIndex.value = value }
    fun updateResizeHandleIndex(value: String?) { _resizeHandleIndex.value = value }
    fun updateDraggingCanvas(value: Pair<Pair<Double, Double>, Pair<Double, Double>>?) { _draggingCanvas.value = value }
    fun updateSelectedItem(value: SpaceItem?) { _selectedItem.value = value }
    fun updateDraggedOffset(value: Pair<Double, Double>) { _draggedOffset.value = value }
    fun updateOffset(value: Pair<Double, Double>) { _offset.value = value }
    fun updateScribblePoints(value: MutableList<Pair<Double, Double>>) { _scribblePoints.value = value }
    fun updateMousePosition(value: Pair<Double, Double>?) { _mousePosition.value = value }
    /**
     * Updates the entire data and manages stable item IDs by index.
     */
    fun updateData(value: SpaceData?) {
        val oldItems = _data.value?.items ?: emptyList()
        val oldStable = _stableIds.value
        val newItems = value?.items ?: emptyList()
        val newStable = mutableListOf<String>()
        for (i in newItems.indices) {
            if (i < oldItems.size) {
                newStable.add(oldStable.getOrNull(i) ?: generateStableId())
            } else {
                newStable.add(generateStableId())
            }
        }
        _stableIds.value = newStable
        _data.value = value
    }
    fun updateCardId(value: String?) { _cardId.value = value }
    fun updateCanEdit(value: Boolean) { _canEdit.value = value }
    fun updateDirty(value: Int?) { _dirty.value = value }
    fun updateDrawFunc(value: () -> Unit) { _drawFunc.value = value }
    /**
     * Updates the duration (in milliseconds) for the slide at the given index.
     */
    fun updateSlideDuration(index: Int, durationMs: Long) {
        val slideItem = _slides.value.getOrNull(index) ?: return
        val content = slideItem.content as? SpaceContent.Slide ?: return
        val updatedSlide = slideItem.copy(content = content.copy(duration = durationMs))
        val idx = items.indexOf(slideItem)
        if (idx >= 0) {
            val newItems = items.toMutableList().apply { set(idx, updatedSlide) }
            updateData(data?.copy(items = newItems) ?: SpaceData(items = newItems))
            updateDirty(nextInt())
            _slides.value = collectAllSlides()
        }
    }
    /**
     * Gets the duration (in milliseconds) for the slide at the given index.
     */
    fun getSlideDuration(index: Int): Long? {
        val slideItem = _slides.value.getOrNull(index) ?: return null
        val content = slideItem.content as? SpaceContent.Slide ?: return null
        return content.duration
    }

    @Composable
    fun collectTool() = _tool.collectAsState().value

    @Composable
    fun collectDrawInfo() = _drawInfo.collectAsState().value

    @Composable
    fun collectDraggedIndex() = _draggedIndex.collectAsState().value

    @Composable
    fun collectResizeHandleIndex() = _resizeHandleIndex.collectAsState().value

    @Composable
    fun collectDraggingCanvas() = _draggingCanvas.collectAsState().value

    @Composable
    fun collectSelectedItem() = _selectedItem.collectAsState().value

    @Composable
    fun collectDraggedOffset() = _draggedOffset.collectAsState().value

    @Composable
    fun collectOffset() = _offset.collectAsState().value

    @Composable
    fun collectScribblePoints() = _scribblePoints.collectAsState().value

    @Composable
    fun collectMousePosition() = _mousePosition.collectAsState().value

    @Composable
    fun collectData() = _data.collectAsState().value

    @Composable
    fun collectCardId() = _cardId.collectAsState().value

    @Composable
    fun collectCanEdit() = _canEdit.collectAsState().value

    @Composable
    fun collectDirty() = _dirty.collectAsState().value

    @Composable
    fun collectDrawFunc() = _drawFunc.collectAsState().value

    // Slideshow state collection methods
    @Composable
    fun collectSlideshowMode() = _slideshowMode.collectAsState().value

    @Composable
    fun collectCurrentSlideIndex() = _currentSlideIndex.collectAsState().value

    @Composable
    fun collectSlides() = _slides.collectAsState().value

    @Composable
    fun collectItemVisibility() = _itemVisibility.collectAsState().value

    @Composable
    fun collectSlideshowPaused() = _slideshowPaused.collectAsState().value

    // Helper property to access items
    private val items get() = data?.items ?: listOf()

    // Slideshow management methods

    /**
     * Creates a new slide with the given title and selected items
     */
    fun createSlide(title: String) {
        val newSlide = SpaceItem(
            content = SpaceContent.Slide(
                page = cardId,
                title = title,
                items = selectedItem?.let { listOf(getItemId(it)) } ?: emptyList()
            ),
            position = 0.0 to 0.0 // Position doesn't matter for slides
        )

        val newItems = items + newSlide
        updateData(data?.copy(items = newItems) ?: SpaceData(items = newItems))
        updateDirty(nextInt())
        // Update slideshow slides and select the new slide
        val updatedSlides = collectAllSlides()
        _slides.value = updatedSlides
        val newIndex = updatedSlides.lastIndex
        _currentSlideIndex.value = newIndex
        showSlide(newIndex)
        // Set default duration for new slide
        val slideItem = _slides.value.getOrNull(newIndex)
    }

    /**
     * Renames the slide at the given index.
     */
    fun renameSlide(index: Int, newTitle: String) {
        val slideItem = _slides.value.getOrNull(index) ?: return
        val content = (slideItem.content as? SpaceContent.Slide) ?: return
        val updatedSlide = slideItem.copy(content = content.copy(title = newTitle))
        val idx = items.indexOf(slideItem)
        if (idx >= 0) {
            val newItems = items.toMutableList().apply { set(idx, updatedSlide) }
            updateData(data?.copy(items = newItems) ?: SpaceData(items = newItems))
            updateDirty(nextInt())
            _slides.value = collectAllSlides()
        }
    }

    /**
     * Deletes the slide at the given index.
     */
    fun deleteSlide(index: Int) {
        val slideItem = _slides.value.getOrNull(index) ?: return
        val newItems = items.filter { it != slideItem }
        updateData(data?.copy(items = newItems) ?: SpaceData(items = newItems))
        updateDirty(nextInt())
        val updatedSlides = collectAllSlides()
        _slides.value = updatedSlides
        if (updatedSlides.isEmpty()) {
            // Remain in slideshow mode even if no slides remain
            // Clear visibility to show all page items
            _itemVisibility.value = emptyMap()
        } else {
            val newIndex = if (index < updatedSlides.size) index else updatedSlides.lastIndex
            _currentSlideIndex.value = newIndex
            showSlide(newIndex)
        }
    }

    /**
     * Adds an item to the current slide
     */
    fun addItemToSlide(slideItem: SpaceItem, itemToAdd: SpaceItem) {
        val slideContent = slideItem.content as? SpaceContent.Slide ?: return
        val itemId = getItemId(itemToAdd)

        if (slideContent.items.contains(itemId)) return

        val updatedSlide = SpaceItem(
            content = slideContent.copy(
                items = slideContent.items + itemId
            ),
            position = slideItem.position
        )

        val index = items.indexOf(slideItem)
        if (index >= 0) {
            val newItems = items.toMutableList()
            newItems[index] = updatedSlide
            updateData(data?.copy(items = newItems) ?: SpaceData(items = newItems))
            updateDirty(nextInt())
            // Update slide list and refresh visibility to include the new item
            _slides.value = collectAllSlides()
            showSlide(_currentSlideIndex.value)
        }
    }

    /**
     * Removes an item from the current slide
     */
    fun removeItemFromSlide(slideItem: SpaceItem, itemToRemove: SpaceItem) {
        val slideContent = slideItem.content as? SpaceContent.Slide ?: return
        val itemId = getItemId(itemToRemove)

        val updatedSlide = SpaceItem(
            content = slideContent.copy(
                items = slideContent.items.filter { it != itemId }
            ),
            position = slideItem.position
        )

        val index = items.indexOf(slideItem)
        if (index >= 0) {
            val newItems = items.toMutableList()
            newItems[index] = updatedSlide
            updateData(data?.copy(items = newItems) ?: SpaceData(items = newItems))
            updateDirty(nextInt())
        }
    }

    /**
     * Collects all slides for the current page
     */
    fun collectAllSlides(): List<SpaceItem> {
        return items.filter { 
            val content = it.content as? SpaceContent.Slide ?: return@filter false
            content.page == null || content.page == cardId
        }
    }

    /**
     * Starts the slideshow
     */
    fun startSlideshow() {
        val slides = collectAllSlides()
        _slideshowMode.value = true
        _slideshowPaused.value = true
        _currentSlideIndex.value = if (slides.isNotEmpty()) 0 else -1
        _slides.value = slides

        // Save current view state to restore later
        _savedViewState.value = ViewState(
            offset = offset,
            selectedItem = selectedItem
        )

        // Clear selection
        updateSelectedItem(null)

        // Show the first slide if available
        if (slides.isNotEmpty()) {
            showSlide(0)
        } else {
            // No slides: reset visibility map
            _itemVisibility.value = emptyMap()
        }
    }

    /**
     * Stops the slideshow
     */
    fun stopSlideshow() {
        _slideshowMode.value = false
        _slideshowPaused.value = false

        // Reset item visibility
        _itemVisibility.value = emptyMap()

        // Restore previous view state
        _savedViewState.value?.let {
            updateOffset(it.offset)
            updateSelectedItem(it.selectedItem)
        }
    }

    /**
     * Pauses the slideshow without exiting slideshow mode
     */
    fun pauseSlideshow() {
        _slideshowPaused.value = true
    }

    /**
     * Resumes the slideshow from paused state
     */
    fun resumeSlideshow() {
        _slideshowPaused.value = false
    }

    /**
     * Shows the next slide
     */
    fun nextSlide() {
        val nextIndex = (_currentSlideIndex.value + 1) % _slides.value.size
        _currentSlideIndex.value = nextIndex
        showSlide(nextIndex)
    }

    /**
     * Shows the previous slide
     */
    fun previousSlide() {
        val prevIndex = if (_currentSlideIndex.value > 0) 
                          _currentSlideIndex.value - 1 
                        else 
                          _slides.value.size - 1
        _currentSlideIndex.value = prevIndex
        showSlide(prevIndex)
    }

    /**
     * Updates the current slide index
     */
    fun updateCurrentSlideIndex(index: Int) {
        _currentSlideIndex.value = index
    }

    /**
     * Shows a specific slide by index
     */
    fun showSlide(index: Int) {
        val slide = _slides.value.getOrNull(index) ?: return
        val content = slide.content as? SpaceContent.Slide ?: return

        // Create a new visibility map
        val newVisibility = mutableMapOf<String, Boolean>()

        // Set visibility for all items
        items.forEach { item ->
            if (item.content is SpaceContent.Slide) return@forEach

            // Only process items visible on current page
            if (item.content !is SpaceContent.Page && 
                (getItemPage(item) == null || getItemPage(item) == cardId)) {

                val itemId = getItemId(item)
                val isInSlide = content.items.contains(itemId)

                // Set visibility in the map
                newVisibility[itemId] = isInSlide
            }
        }

        // Update the visibility map
        _itemVisibility.value = newVisibility
    }

    /**
     * Gets the stable ID of an item based on its index.
     */
    private fun getItemId(item: SpaceItem): String {
        val items = _data.value?.items ?: return ""
        val idx = items.indexOf(item)
        return _stableIds.value.getOrNull(idx) ?: ""
    }

    /**
     * Gets the page of an item
     */
    private fun getItemPage(item: SpaceItem): String? {
        return when (val content = item.content) {
            is SpaceContent.Text -> content.page
            is SpaceContent.Line -> content.page
            is SpaceContent.Box -> content.page
            is SpaceContent.Circle -> content.page
            is SpaceContent.Scribble -> content.page
            is SpaceContent.Photo -> content.page
            is SpaceContent.Slide -> content.page
            is SpaceContent.Page -> null
        }
    }

    // Helper method to check if an item should be visible
    fun isItemVisible(item: SpaceItem): Boolean {
        val itemId = getItemId(item)
        if (slideshowMode) {
            // In slideshow mode, only show page items and those in the current slide
            if (item.content is SpaceContent.Slide) return false
            if (item.content is SpaceContent.Page) return true
            return itemVisibility[itemId] ?: false
        } else {
            // In normal mode, hide slide definitions and any items belonging to slides
            if (item.content is SpaceContent.Slide) return false
            if (item.content is SpaceContent.Page) return true
            // Collect all slide item IDs
            val slideItemIds = items
                .mapNotNull { it.content as? SpaceContent.Slide }
                .flatMap { it.items }
                .toSet()
            return !slideItemIds.contains(itemId)
        }
    }

    fun onMouseDown(rect: DOMRect, clientX: Double, clientY: Double) {
        val mouseX = clientX - rect.left - offset.first
        val mouseY = clientY - rect.top - offset.second

        when (tool) {
            SpaceWidgetTool.Scribble -> {
                // Clear previous points and start a new scribble
                scribblePoints.clear()
                scribblePoints.add(mouseX to mouseY)

                // Initialize drawInfo for preview
                drawInfo = DrawInfo(
                    tool = tool, 
                    from = mouseX to mouseY,
                    points = listOf(mouseX to mouseY)
                )
            }

            SpaceWidgetTool.Default -> {
                var foundItem = false
                items.forEachIndexed { index, (item, position) ->
                    val (x, y) = position
                    // Check if we're clicking on a resize handle of the selected item
                    if (selectedItem?.content == item && canEdit) {
                        // Check which resize handle is being clicked
                        when (item) {
                            is SpaceContent.Line -> {
                                // Start point
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "start"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // End point
                                if (sqrt((mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "end"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                            }
                            is SpaceContent.Box -> {
                                // Top-left
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "topLeft"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Top-right
                                if (sqrt((mouseX - item.to.first).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "topRight"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom-left
                                if (sqrt((mouseX - x).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottomLeft"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom-right
                                if (sqrt((mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottomRight"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                            }
                            is SpaceContent.Circle -> {
                                val radiusX = (item.to.first - x).absoluteValue
                                val radiusY = (item.to.second - y).absoluteValue

                                // Center
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "center"
                                    draggedIndex = index
                                    draggedOffset = (mouseX - x) to (mouseY - y)
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Right
                                if (sqrt((mouseX - (x + radiusX)).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "right"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Left
                                if (sqrt((mouseX - (x - radiusX)).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "left"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom
                                if (sqrt((mouseX - x).pow(2) + (mouseY - (y + radiusY)).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottom"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Top
                                if (sqrt((mouseX - x).pow(2) + (mouseY - (y - radiusY)).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "top"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                            }
                            is SpaceContent.Photo -> {
                                val width = item.width?.toDouble() ?: 200.0
                                val height = item.height?.toDouble() ?: 150.0
                                val scaleX = (item.to.first - x).absoluteValue / width
                                val scaleY = (item.to.second - y).absoluteValue / height

                                // Top-left
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "topLeft"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Top-right
                                if (sqrt((mouseX - (x + width * scaleX)).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "topRight"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom-left
                                if (sqrt((mouseX - x).pow(2) + (mouseY - (y + height * scaleY)).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottomLeft"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom-right
                                if (sqrt((mouseX - (x + width * scaleX)).pow(2) + (mouseY - (y + height * scaleY)).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottomRight"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                            }

                            is SpaceContent.Scribble -> {
                                // Top-left (position)
                                if (sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "topLeft"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Top-right
                                if (sqrt((mouseX - item.to.first).pow(2) + (mouseY - y).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "topRight"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom-left
                                if (sqrt((mouseX - x).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottomLeft"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                                // Bottom-right (to)
                                if (sqrt((mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0) {
                                    resizeHandleIndex = "bottomRight"
                                    draggedIndex = index
                                    foundItem = true
                                    return@forEachIndexed
                                }
                            }
                            else -> {}
                        }
                    }

                    // If not clicking on a resize handle, check if clicking on the item itself
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

                            is SpaceContent.Photo -> {
                                // Check if mouse is within the bounds of the photo
                                val width = item.width?.toDouble() ?: 200.0
                                val height = item.height?.toDouble() ?: 150.0
                                val scaleX = (item.to.first - x).absoluteValue / width
                                val scaleY = (item.to.second - y).absoluteValue / height
                                val scaledWidth = width * scaleX
                                val scaledHeight = height * scaleY

                                // Check if mouse is within the photo bounds
                                (mouseX >= x && mouseX <= x + scaledWidth &&
                                        mouseY >= y && mouseY <= y + scaledHeight) ||
                                // Or near the corners for resizing
                                sqrt((mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0 ||
                                sqrt((mouseX - item.to.first).pow(2) + (mouseY - y).pow(2)) <= 24.0 ||
                                sqrt((mouseX - x).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0 ||
                                sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0
                            }

                            is SpaceContent.Scribble -> {
                                // Check if mouse is near any of the four corners of the scribble's bounding box
                                sqrt((mouseX - item.to.first).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0 || // Bottom-right
                                sqrt((mouseX - item.to.first).pow(2) + (mouseY - y).pow(2)) <= 24.0 || // Top-right
                                sqrt((mouseX - x).pow(2) + (mouseY - item.to.second).pow(2)) <= 24.0 || // Bottom-left
                                sqrt((mouseX - x).pow(2) + (mouseY - y).pow(2)) <= 24.0 // Top-left (already checked outside)
                            }

                            else -> false
                        }
                    ) {
                        foundItem = true
                        if (canEdit && resizeHandleIndex == null) {
                            draggedIndex = index
                            draggedOffset = (mouseX - x) to (mouseY - y)
                        }
                        selectedItem = items[index]
                    }
                }

                if (!foundItem) {
                    draggingCanvas = offset to (clientX to clientY)
                    selectedItem = null
                }

                drawFunc()
            }

            SpaceWidgetTool.Text,
            SpaceWidgetTool.Photo -> Unit

            SpaceWidgetTool.Line,
            SpaceWidgetTool.Box,
            SpaceWidgetTool.Circle,
                -> {
                drawInfo = DrawInfo(tool = tool, from = mouseX to mouseY)
            }

        }
    }

    fun onMouseMove(rect: DOMRect, clientX: Double, clientY: Double) {
        val mouseX = clientX - rect.left - offset.first
        val mouseY = clientY - rect.top - offset.second
        val newX = mouseX - draggedOffset.first
        val newY = mouseY - draggedOffset.second
        updateMousePosition(mouseX to mouseY)

        // Redraw to show hover effects
        drawFunc()

        when (tool) {
            SpaceWidgetTool.Scribble -> {
                drawInfo?.let {
                    scribblePoints.add(mouseX to mouseY)

                    // Update drawInfo for preview with all points
                    updateDrawInfo(
                        it.copy(
                            to = mouseX to mouseY,
                            points = scribblePoints.toList()
                        )
                    )
                    drawFunc()
                }
            }

            SpaceWidgetTool.Default -> {
                if (canEdit) {
                    draggedIndex?.let { index ->
                        // If we're dragging a resize handle
                        if (resizeHandleIndex != null) {
                            val item = data!!.items!![index]
                            val (content, position) = item
                            val (x, y) = position

                            // Resize based on the content type and handle being dragged
                            when (content) {
                                is SpaceContent.Line -> {
                                    when (resizeHandleIndex) {
                                        "start" -> {
                                            // Update the start point (position)
                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = content,
                                                                position = mouseX to mouseY
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "end" -> {
                                            // Update the end point (to)
                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Line(
                                                                    page = content.page,
                                                                    to = mouseX to mouseY
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }

                                is SpaceContent.Box -> {
                                    val origW = (content.to.first - position.first).absoluteValue
                                    val origH = (content.to.second - position.second).absoluteValue
                                    when (resizeHandleIndex) {
                                        "topLeft" -> {
                                            if (shiftDown) {
                                                val anchorX = content.to.first
                                                val anchorY = content.to.second
                                                val dx = anchorX - mouseX
                                                val dy = anchorY - mouseY
                                                val s = (dx.absoluteValue / origW).coerceAtMost(dy.absoluteValue / origH)
                                                val newW = origW * s
                                                val newH = origH * s
                                                val signX = if (dx >= 0) 1.0 else -1.0
                                                val signY = if (dy >= 0) 1.0 else -1.0
                                                val newPosX = anchorX - signX * newW
                                                val newPosY = anchorY - signY * newH
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(index, SpaceItem(content = content, position = newPosX to newPosY))
                                                        }
                                                    )
                                                )
                                            } else {
                                                // Update the top-left corner (position)
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = content,
                                                                    position = mouseX to mouseY
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            }
                                        }

                                        "topRight" -> {
                                            // Update the top-right corner (x of to, y of position)
                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Box(
                                                                    page = content.page,
                                                                    to = mouseX to content.to.second
                                                                ),
                                                                position = position.first to mouseY
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "bottomLeft" -> {
                                            // Update the bottom-left corner (x of position, y of to)
                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Box(
                                                                    page = content.page,
                                                                    to = content.to.first to mouseY
                                                                ),
                                                                position = mouseX to position.second
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "bottomRight" -> {
                                            // Update the bottom-right corner (to)
                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Box(
                                                                    page = content.page,
                                                                    to = mouseX to mouseY
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }

                                is SpaceContent.Circle -> {
                                    when (resizeHandleIndex) {
                                        "center" -> {
                                            // Move the center (position) while maintaining the radius
                                            val radiusX = (content.to.first - position.first).absoluteValue
                                            val radiusY =
                                                (content.to.second - position.second).absoluteValue

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Circle(
                                                                    page = content.page,
                                                                    to = (newX + radiusX) to (newY + radiusY)
                                                                ),
                                                                position = newX to newY
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "right" -> {
                                            // Update the right handle (x of to)
                                            val radiusX = (mouseX - position.first).absoluteValue
                                            val radiusY =
                                                (content.to.second - position.second).absoluteValue

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Circle(
                                                                    page = content.page,
                                                                    to = (position.first + radiusX) to content.to.second
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "left" -> {
                                            // Update the left handle (x of to)
                                            val radiusX = (mouseX - position.first).absoluteValue
                                            val radiusY =
                                                (content.to.second - position.second).absoluteValue

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Circle(
                                                                    page = content.page,
                                                                    to = (position.first + radiusX) to content.to.second
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "bottom" -> {
                                            // Update the bottom handle (y of to)
                                            val radiusX = (content.to.first - position.first).absoluteValue
                                            val radiusY = (mouseY - position.second).absoluteValue

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Circle(
                                                                    page = content.page,
                                                                    to = content.to.first to (position.second + radiusY)
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "top" -> {
                                            // Update the top handle (y of to)
                                            val radiusX = (content.to.first - position.first).absoluteValue
                                            val radiusY = (mouseY - position.second).absoluteValue

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Circle(
                                                                    page = content.page,
                                                                    to = content.to.first to (position.second + radiusY)
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }

                                is SpaceContent.Photo -> {
                                    val origW = content.width?.toDouble() ?: 200.0
                                    val origH = content.height?.toDouble() ?: 150.0

                                    when (resizeHandleIndex) {
                                        "topLeft" -> {
                                            if (shiftDown) {
                                                val anchorX = content.to.first
                                                val anchorY = content.to.second
                                                val dx = anchorX - mouseX
                                                val dy = anchorY - mouseY
                                                val s = (dx.absoluteValue / origW).coerceAtMost(dy.absoluteValue / origH)
                                                val newW = origW * s
                                                val newH = origH * s
                                                val signX = if (dx >= 0) 1.0 else -1.0
                                                val signY = if (dy >= 0) 1.0 else -1.0
                                                val newPosX = anchorX - signX * newW
                                                val newPosY = anchorY - signY * newH
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = content.to
                                                                    ),
                                                                    position = newPosX to newPosY
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            } else {
                                                // Update the top-left corner (position)
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = content,
                                                                    position = mouseX to mouseY
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            }
                                        }

                                        "topRight" -> {
                                            if (shiftDown) {
                                                val anchorX = position.first
                                                val anchorY = content.to.second
                                                val dx = mouseX - anchorX
                                                val dy = anchorY - mouseY
                                                val s = (dx.absoluteValue / origW).coerceAtMost(dy.absoluteValue / origH)
                                                val newW = origW * s
                                                val newH = origH * s
                                                val signX = if (dx >= 0) 1.0 else -1.0
                                                val signY = if (dy >= 0) 1.0 else -1.0
                                                val newToX = anchorX + signX * newW
                                                val newPosY = anchorY - signY * newH
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index,
                                                                SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = newToX to anchorY
                                                                    ),
                                                                    position = anchorX to newPosY
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            } else {
                                                // Update the top-right corner
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = mouseX to content.to.second
                                                                    ),
                                                                    position = position.first to mouseY
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            }
                                        }

                                        "bottomLeft" -> {
                                            if (shiftDown) {
                                                val anchorX = content.to.first
                                                val anchorY = position.second
                                                val dx = anchorX - mouseX
                                                val dy = mouseY - anchorY
                                                val s = (dx.absoluteValue / origW).coerceAtMost(dy.absoluteValue / origH)
                                                val newW = origW * s
                                                val newH = origH * s
                                                val signX = if (dx >= 0) 1.0 else -1.0
                                                val signY = if (dy >= 0) 1.0 else -1.0
                                                val newPosX = anchorX - signX * newW
                                                val newToY = anchorY + signY * newH
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index,
                                                                SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = anchorX to newToY
                                                                    ),
                                                                    position = newPosX to anchorY
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            } else {
                                                // Update the bottom-left corner
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = content.to.first to mouseY
                                                                    ),
                                                                    position = mouseX to position.second
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            }
                                        }

                                        "bottomRight" -> {
                                            if (shiftDown) {
                                                val anchorX = position.first
                                                val anchorY = position.second
                                                val dx = mouseX - anchorX
                                                val dy = mouseY - anchorY
                                                val s = (dx.absoluteValue / origW).coerceAtMost(dy.absoluteValue / origH)
                                                val newW = origW * s
                                                val newH = origH * s
                                                val signX = if (dx >= 0) 1.0 else -1.0
                                                val signY = if (dy >= 0) 1.0 else -1.0
                                                val newToX = anchorX + signX * newW
                                                val newToY = anchorY + signY * newH
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = newToX to newToY
                                                                    ),
                                                                    position = position
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            } else {
                                                // Update the bottom-right corner (to)
                                                updateData(
                                                    data!!.copy(
                                                        items = data!!.items!!.toMutableList().apply {
                                                            removeAt(index)
                                                            add(
                                                                index, SpaceItem(
                                                                    content = SpaceContent.Photo(
                                                                        page = content.page,
                                                                        photo = content.photo,
                                                                        width = content.width,
                                                                        height = content.height,
                                                                        to = mouseX to mouseY
                                                                    ),
                                                                    position = position
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                is SpaceContent.Scribble -> {
                                    when (resizeHandleIndex) {
                                        "topLeft" -> {
                                            // Calculate the scale factors
                                            val oldWidth = content.to.first - position.first
                                            val oldHeight = content.to.second - position.second
                                            val newWidth = content.to.first - mouseX
                                            val newHeight = content.to.second - mouseY
                                            // Use absoluteValue to handle negative scaling properly
                                            // Apply a minimum scale factor to prevent disappearing when scaled to near 0
                                            val minScaleFactor =
                                                0.05 // Minimum scale factor to prevent disappearing
                                            val scaleX =
                                                if (oldWidth != 0.0) (newWidth.absoluteValue / oldWidth.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            val scaleY =
                                                if (oldHeight != 0.0) (newHeight.absoluteValue / oldHeight.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            // Determine if we need to flip the coordinates
                                            val flipX = newWidth < 0 != oldWidth < 0
                                            val flipY = newHeight < 0 != oldHeight < 0

                                            // Adjust the new width and height based on the minimum scale factor
                                            val adjustedNewWidth =
                                                oldWidth * scaleX * (if (flipX) -1 else 1)
                                            val adjustedNewHeight =
                                                oldHeight * scaleY * (if (flipY) -1 else 1)
                                            val adjustedMouseX = content.to.first - adjustedNewWidth
                                            val adjustedMouseY = content.to.second - adjustedNewHeight

                                            // Scale all points relative to the bottom-right corner
                                            val scaledPoints = content.points.map { point ->
                                                val relativeX = point.first - position.first
                                                val relativeY = point.second - position.second
                                                val newRelativeX =
                                                    relativeX * scaleX * (if (flipX) -1 else 1)
                                                val newRelativeY =
                                                    relativeY * scaleY * (if (flipY) -1 else 1)
                                                (adjustedMouseX + newRelativeX) to (adjustedMouseY + newRelativeY)
                                            }

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Scribble(
                                                                    page = content.page,
                                                                    points = scaledPoints,
                                                                    to = content.to
                                                                ),
                                                                position = adjustedMouseX to adjustedMouseY
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "topRight" -> {
                                            // Calculate the scale factors
                                            val oldWidth = content.to.first - position.first
                                            val oldHeight = content.to.second - position.second
                                            val newWidth = mouseX - position.first
                                            val newHeight = content.to.second - mouseY
                                            // Use absoluteValue to handle negative scaling properly
                                            // Apply a minimum scale factor to prevent disappearing when scaled to near 0
                                            val minScaleFactor =
                                                0.05 // Minimum scale factor to prevent disappearing
                                            val scaleX =
                                                if (oldWidth != 0.0) (newWidth.absoluteValue / oldWidth.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            val scaleY =
                                                if (oldHeight != 0.0) (newHeight.absoluteValue / oldHeight.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            // Determine if we need to flip the coordinates
                                            val flipX = newWidth < 0 != oldWidth < 0
                                            val flipY = newHeight < 0 != oldHeight < 0

                                            // Adjust the new width and height based on the minimum scale factor
                                            val adjustedNewWidth =
                                                oldWidth * scaleX * (if (flipX) -1 else 1)
                                            val adjustedNewHeight =
                                                oldHeight * scaleY * (if (flipY) -1 else 1)
                                            val adjustedMouseX = position.first + adjustedNewWidth
                                            val adjustedMouseY = content.to.second - adjustedNewHeight

                                            // Scale all points relative to the bottom-left corner
                                            val scaledPoints = content.points.map { point ->
                                                val relativeX = point.first - position.first
                                                val relativeY = point.second - position.second
                                                val newRelativeX =
                                                    relativeX * scaleX * (if (flipX) -1 else 1)
                                                val newRelativeY =
                                                    relativeY * scaleY * (if (flipY) -1 else 1)
                                                (position.first + newRelativeX) to (adjustedMouseY + newRelativeY)
                                            }

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Scribble(
                                                                    page = content.page,
                                                                    points = scaledPoints,
                                                                    to = adjustedMouseX to content.to.second
                                                                ),
                                                                position = position.first to adjustedMouseY
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "bottomLeft" -> {
                                            // Calculate the scale factors
                                            val oldWidth = content.to.first - position.first
                                            val oldHeight = content.to.second - position.second
                                            val newWidth = content.to.first - mouseX
                                            val newHeight = mouseY - position.second
                                            // Use absoluteValue to handle negative scaling properly
                                            // Apply a minimum scale factor to prevent disappearing when scaled to near 0
                                            val minScaleFactor =
                                                0.05 // Minimum scale factor to prevent disappearing
                                            val scaleX =
                                                if (oldWidth != 0.0) (newWidth.absoluteValue / oldWidth.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            val scaleY =
                                                if (oldHeight != 0.0) (newHeight.absoluteValue / oldHeight.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            // Determine if we need to flip the coordinates
                                            val flipX = newWidth < 0 != oldWidth < 0
                                            val flipY = newHeight < 0 != oldHeight < 0

                                            // Adjust the new width and height based on the minimum scale factor
                                            val adjustedNewWidth =
                                                oldWidth * scaleX * (if (flipX) -1 else 1)
                                            val adjustedNewHeight =
                                                oldHeight * scaleY * (if (flipY) -1 else 1)
                                            val adjustedMouseX = content.to.first - adjustedNewWidth
                                            val adjustedMouseY = position.second + adjustedNewHeight

                                            // Scale all points relative to the top-right corner
                                            val scaledPoints = content.points.map { point ->
                                                val relativeX = point.first - position.first
                                                val relativeY = point.second - position.second
                                                val newRelativeX =
                                                    relativeX * scaleX * (if (flipX) -1 else 1)
                                                val newRelativeY =
                                                    relativeY * scaleY * (if (flipY) -1 else 1)
                                                (adjustedMouseX + newRelativeX) to (position.second + newRelativeY)
                                            }

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Scribble(
                                                                    page = content.page,
                                                                    points = scaledPoints,
                                                                    to = content.to.first to adjustedMouseY
                                                                ),
                                                                position = adjustedMouseX to position.second
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        "bottomRight" -> {
                                            // Calculate the scale factors
                                            val oldWidth = content.to.first - position.first
                                            val oldHeight = content.to.second - position.second
                                            val newWidth = mouseX - position.first
                                            val newHeight = mouseY - position.second
                                            // Use absoluteValue to handle negative scaling properly
                                            // Apply a minimum scale factor to prevent disappearing when scaled to near 0
                                            val minScaleFactor =
                                                0.05 // Minimum scale factor to prevent disappearing
                                            val scaleX =
                                                if (oldWidth != 0.0) (newWidth.absoluteValue / oldWidth.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            val scaleY =
                                                if (oldHeight != 0.0) (newHeight.absoluteValue / oldHeight.absoluteValue).coerceAtLeast(
                                                    minScaleFactor
                                                ) else 1.0
                                            // Determine if we need to flip the coordinates
                                            val flipX = newWidth < 0 != oldWidth < 0
                                            val flipY = newHeight < 0 != oldHeight < 0

                                            // Adjust the new width and height based on the minimum scale factor
                                            val adjustedNewWidth =
                                                oldWidth * scaleX * (if (flipX) -1 else 1)
                                            val adjustedNewHeight =
                                                oldHeight * scaleY * (if (flipY) -1 else 1)
                                            val adjustedMouseX = position.first + adjustedNewWidth
                                            val adjustedMouseY = position.second + adjustedNewHeight

                                            // Scale all points relative to the top-left corner
                                            val scaledPoints = content.points.map { point ->
                                                val relativeX = point.first - position.first
                                                val relativeY = point.second - position.second
                                                val newRelativeX =
                                                    relativeX * scaleX * (if (flipX) -1 else 1)
                                                val newRelativeY =
                                                    relativeY * scaleY * (if (flipY) -1 else 1)
                                                (position.first + newRelativeX) to (position.second + newRelativeY)
                                            }

                                            updateData(
                                                data!!.copy(
                                                    items = data!!.items!!.toMutableList().apply {
                                                        removeAt(index)
                                                        add(
                                                            index, SpaceItem(
                                                                content = SpaceContent.Scribble(
                                                                    page = content.page,
                                                                    points = scaledPoints,
                                                                    to = adjustedMouseX to adjustedMouseY
                                                                ),
                                                                position = position
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    updateData(
                                        data!!.copy(
                                            items = data!!.items!!.toMutableList().apply {
                                                val item = removeAt(index)
                                                add(index, item.move(newX to newY))
                                            }
                                        )
                                    )
                                }
                            }

                            updateDirty(nextInt())
                            drawFunc()
                        } else {
                            updateData(
                                data!!.copy(
                                    items = data!!.items!!.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index, item.move(newX to newY))
                                    }
                                )
                            )
                            updateDirty(nextInt())
                            drawFunc()
                        }
                    }
                }

                draggingCanvas?.let { (initialOffset, initialMouse) ->
                    updateOffset(
                        (initialOffset.first + (clientX - initialMouse.first)) to (initialOffset.second + (clientY - initialMouse.second))
                    )
                    drawFunc()
                }
            }

            SpaceWidgetTool.Text,
            SpaceWidgetTool.Photo -> Unit

            SpaceWidgetTool.Line,
            SpaceWidgetTool.Box,
            SpaceWidgetTool.Circle,
                -> {
                drawInfo?.let {
                    updateDrawInfo(
                        it.copy(to = newX to newY)
                    )
                    drawFunc()
                }
            }

        }

    }

    fun onMouseUp() {
        when (tool) {
            SpaceWidgetTool.Scribble -> {
                drawInfo?.from?.let { from ->
                    if (scribblePoints.size > 1) {
                        val minX = scribblePoints.minOf { it.first }
                        val minY = scribblePoints.minOf { it.second }
                        val maxX = scribblePoints.maxOf { it.first }
                        val maxY = scribblePoints.maxOf { it.second }
                        val to = maxX to maxY
                        val newItem = SpaceItem(
                            content = SpaceContent.Scribble(
                                page = cardId,
                                points = scribblePoints.toList(),
                                to = to
                            ),
                            position = minX to minY
                        )
                        updateData(data!!.copy(items = items + newItem))
                        updateDirty(nextInt())
                        if (slideshowMode) {
                            slides.getOrNull(currentSlideIndex)?.let { slideItem ->
                                addItemToSlide(slideItem, newItem)
                            }
                        }
                    }
                    scribblePoints.clear()
                    drawInfo = null
                    drawFunc()
                }
            }

            SpaceWidgetTool.Default -> {
                draggedIndex = null
                resizeHandleIndex = null
                draggingCanvas = null
                drawFunc()
            }

            SpaceWidgetTool.Text,
            SpaceWidgetTool.Photo -> Unit

            SpaceWidgetTool.Line,
            SpaceWidgetTool.Box,
            SpaceWidgetTool.Circle,
                -> {
                drawInfo?.from?.let { from ->
                    drawInfo?.to?.let { to ->
                        val content = when (drawInfo?.tool) {
                            SpaceWidgetTool.Box -> SpaceContent.Box(page = cardId, to = to)
                            SpaceWidgetTool.Circle -> SpaceContent.Circle(page = cardId, to = to)
                            else -> SpaceContent.Line(page = cardId, to = to)
                        }
                        val newItem = SpaceItem(content = content, position = from)
                        updateData(data!!.copy(items = items + newItem))
                        updateDirty(nextInt())
                        if (slideshowMode) {
                            slides.getOrNull(currentSlideIndex)?.let { slideItem ->
                                addItemToSlide(slideItem, newItem)
                            }
                        }
                    }
                }
                drawInfo = null
                drawFunc()
            }

        }
    }

    // Function to handle text input dialog and add text to the canvas
    fun addText(mouseX: Double, mouseY: Double, text: String) {
        if (text.isNotBlank()) {
        val newItem = SpaceItem(
            content = SpaceContent.Text(page = cardId, text),
            position = mouseX to mouseY
        )
        val newItems = data!!.items!!.toMutableList().apply { add(newItem) }
        updateData(data!!.copy(items = newItems))
        updateDirty(nextInt())
        if (slideshowMode) {
            slides.getOrNull(currentSlideIndex)?.let { slideItem ->
                addItemToSlide(slideItem, newItem)
            }
        }
        drawFunc()
        }
    }

    // Function to add a photo to the canvas
    fun addPhoto(mouseX: Double, mouseY: Double, photoUrl: String, width: Int?, height: Int?) {
        // Calculate display size preserving original aspect, fitting within default bounds
        val origW = width?.toDouble() ?: 200.0
        val origH = height?.toDouble() ?: 150.0
        val maxW = 200.0
        val maxH = 200.0
        val scale = (maxW / origW).coerceAtMost(maxH / origH)
        val displayW = origW * scale
        val displayH = origH * scale
        val newItem = SpaceItem(
            content = SpaceContent.Photo(
                page = cardId,
                photo = photoUrl,
                width = width,
                height = height,
                to = (mouseX + displayW) to (mouseY + displayH)
            ),
            position = mouseX to mouseY
        )
        val newItems = data!!.items!!.toMutableList().apply { add(newItem) }
        updateData(data!!.copy(items = newItems))
        updateDirty(nextInt())
        if (slideshowMode) {
            slides.getOrNull(currentSlideIndex)?.let { slideItem ->
                addItemToSlide(slideItem, newItem)
            }
        }
        drawFunc()
    }

    // Function to add a photo at the center of the canvas
    fun addPhotoAtCenter(photoUrl: String, width: Int?, height: Int?, canvasWidth: Double, canvasHeight: Double) {
        // Calculate position in the center of the visible area
        val centerX = canvasWidth / 2 - offset.first
        val centerY = canvasHeight / 2 - offset.second

        // Calculate display size preserving original aspect, fitting within default bounds
        val origW = width?.toDouble() ?: 200.0
        val origH = height?.toDouble() ?: 150.0
        val maxW = 200.0
        val maxH = 200.0
        val scale = (maxW / origW).coerceAtMost(maxH / origH)
        val displayW = origW * scale
        val displayH = origH * scale
        val newItem = SpaceItem(
            content = SpaceContent.Photo(
                page = cardId,
                photo = photoUrl,
                width = width,
                height = height,
                to = (centerX + displayW) to (centerY + displayH)
            ),
            position = centerX to centerY
        )
        val newItems = data!!.items!!.toMutableList().apply { add(newItem) }
        updateData(data!!.copy(items = newItems))
        updateDirty(nextInt())
        if (slideshowMode) {
            slides.getOrNull(currentSlideIndex)?.let { slideItem ->
                addItemToSlide(slideItem, newItem)
            }
        }
        drawFunc()
    }

    // Function to add a new card to the canvas
    fun addCard(mouseX: Double, mouseY: Double, newCardId: String) {
        data = data!!.copy(
            items = data!!.items!!.toMutableList().apply {
                add(SpaceItem(SpaceContent.Page(newCardId), mouseX to mouseY))
            }
        )
        dirty = nextInt()
        drawFunc()
    }

    // Function to update text content
    fun updateText(index: Int, text: String) {
        if (text.isNotBlank()) {
            // Preserve slide membership when updating text
            val oldItem = data!!.items!![index]
            val oldId = getItemId(oldItem)
            val content = oldItem.content as? SpaceContent.Text ?: return
            val newItem = SpaceItem(
                content = SpaceContent.Text(page = content.page, text),
                position = oldItem.position
            )
            // Replace item in main data list
            val updatedItems = data!!.items!!.toMutableList().apply {
                removeAt(index)
                add(index, newItem)
            }
            updateData(data!!.copy(items = updatedItems))
            updateDirty(nextInt())
            // If in slideshow, update current slide definition to replace oldId with newId
            if (slideshowMode) {
                val slidesList = collectAllSlides()
                _slides.value = slidesList
                slidesList.getOrNull(_currentSlideIndex.value)?.let { slideItem ->
                    // Remove old ID and add new one
                    removeItemFromSlide(slideItem, oldItem)
                    addItemToSlide(
                        slidesList[_currentSlideIndex.value],
                        newItem
                    )
                }
            }
            drawFunc()
        }
    }

    // Function to delete the selected item
    fun deleteSelectedItem() {
        if (canEdit && selectedItem != null) {
            data = data!!.copy(
                items = data!!.items!!.filter { it.content != selectedItem?.content }
            )
            dirty = nextInt()
            selectedItem = null
            drawFunc()
        }
    }

    // Function to duplicate the selected item
    fun duplicateSelectedItem() {
        if (canEdit && selectedItem != null) {
            // Create a copy of the selected item with a slight offset
            val offset = 20.0 to 20.0
            val duplicatedItem = selectedItem!!.copy()
                .move(selectedItem!!.position.first + offset.first to selectedItem!!.position.second + offset.second)

            // Add the duplicated item to the data
            data = data!!.copy(
                items = data!!.items!! + duplicatedItem
            )

            // Update the selected item to the new duplicated item
            selectedItem = duplicatedItem

            dirty = nextInt()
            drawFunc()
        }
    }

    // Function to navigate to a card
    fun navigateToCard(newCardId: String, pathItem: SpacePathItem) {
        // Store current state in path item
        // Update cardId and reset offset
        this.cardId = newCardId
        this.offset = 0.0 to 0.0
        this.selectedItem = null
        drawFunc()
    }

    // Function to navigate back to a previous card
    fun navigateBack(pathItem: SpacePathItem) {
        this.cardId = pathItem.id
        this.offset = pathItem.offset
        this.selectedItem = pathItem.selectedItem
        drawFunc()
    }
}

@Composable
fun rememberSpaceWidgetInputControl(): SpaceWidgetInputControl {
    return remember {
        SpaceWidgetInputControl()
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

        is SpaceContent.Scribble -> {
            // Move all points by the offset
            val movedPoints = content.points.map { point ->
                (point.first + offset.first) to (point.second + offset.second)
            }

            copy(
                content = SpaceContent.Scribble(
                    page = content.page,
                    points = movedPoints,
                    to = (content.to.first + offset.first) to (content.to.second + offset.second)
                )
            )
        }

        is SpaceContent.Photo -> {
            copy(
                content = SpaceContent.Photo(
                    page = content.page,
                    photo = content.photo,
                    width = content.width,
                    height = content.height,
                    to = (content.to.first + offset.first) to (content.to.second + offset.second)
                )
            )
        }

        else -> this@move
    }.copy(position = position)
}
