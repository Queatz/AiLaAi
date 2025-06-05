package app.widget.space

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.widget.DrawInfo
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceData
import com.queatz.widgets.widgets.SpaceItem
import kotlinx.coroutines.flow.MutableStateFlow
import org.w3c.dom.DOMRect
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextInt

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
    private val _cardId = MutableStateFlow<String?>(null)
    private val _canEdit = MutableStateFlow(false)
    private val _dirty = MutableStateFlow<Int?>(null)
    private val _drawFunc = MutableStateFlow {}

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
    fun updateData(value: SpaceData?) { _data.value = value }
    fun updateCardId(value: String?) { _cardId.value = value }
    fun updateCanEdit(value: Boolean) { _canEdit.value = value }
    fun updateDirty(value: Int?) { _dirty.value = value }
    fun updateDrawFunc(value: () -> Unit) { _drawFunc.value = value }

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

    private val items get() = data?.items ?: listOf()

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
                                val scaledWidth = width * scaleX.toDouble()
                                val scaledHeight = height * scaleY.toDouble()

                                // Check if mouse is within the photo bounds
                                (mouseX >= x && mouseX <= x + scaledWidth.toDouble() &&
                                        mouseY >= y && mouseY <= y + scaledHeight.toDouble()) ||
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
                                    when (resizeHandleIndex) {
                                        "topLeft" -> {
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
                                    val width = content.width?.toDouble() ?: 200.0
                                    val height = content.height?.toDouble() ?: 150.0

                                    when (resizeHandleIndex) {
                                        "topLeft" -> {
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

                                        "topRight" -> {
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

                                        "bottomLeft" -> {
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

                                        "bottomRight" -> {
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
                        // Calculate bounding box for the scribble
                        val minX = scribblePoints.minOf { it.first }
                        val minY = scribblePoints.minOf { it.second }
                        val maxX = scribblePoints.maxOf { it.first }
                        val maxY = scribblePoints.maxOf { it.second }

                        // Create a to point that represents the bottom-right of the bounding box
                        val to = maxX to maxY

                        // Add the scribble to the data
                        data = data!!.copy(
                            items = items + SpaceItem(
                                content = SpaceContent.Scribble(
                                    page = cardId,
                                    points = scribblePoints.toList(),
                                    to = to
                                ),
                                position = minX to minY
                            )
                        )
                        dirty = nextInt()
                    }

                    // Clear the points and drawing info
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

    // Function to handle text input dialog and add text to the canvas
    fun addText(mouseX: Double, mouseY: Double, text: String) {
        if (text.isNotBlank()) {
            data = data!!.copy(
                items = data!!.items!!.toMutableList().apply {
                    add(SpaceItem(SpaceContent.Text(page = cardId, text), mouseX to mouseY))
                }
            )
            dirty = nextInt()
            drawFunc()
        }
    }

    // Function to add a photo to the canvas
    fun addPhoto(mouseX: Double, mouseY: Double, photoUrl: String, width: Int?, height: Int?) {
        data = data!!.copy(
            items = data!!.items!!.toMutableList().apply {
                add(
                    SpaceItem(
                        SpaceContent.Photo(
                            page = cardId,
                            photo = photoUrl,
                            width = width,
                            height = height,
                            to = (mouseX + 200) to (mouseY + 150)
                        ),
                        mouseX to mouseY
                    )
                )
            }
        )
        dirty = nextInt()
        drawFunc()
    }

    // Function to add a photo at the center of the canvas
    fun addPhotoAtCenter(photoUrl: String, width: Int?, height: Int?, canvasWidth: Double, canvasHeight: Double) {
        // Calculate position in the center of the visible area
        val centerX = canvasWidth / 2 - offset.first
        val centerY = canvasHeight / 2 - offset.second

        data = data!!.copy(
            items = data!!.items!!.toMutableList().apply {
                add(
                    SpaceItem(
                        SpaceContent.Photo(
                            page = cardId,
                            photo = photoUrl,
                            width = width,
                            height = height,
                            to = (centerX + 200) to (centerY + 150)
                        ),
                        centerX to centerY
                    )
                )
            }
        )
        dirty = nextInt()
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
            val item = data!!.items!![index]
            val content = item.content as? SpaceContent.Text ?: return

            data = data!!.copy(
                items = data!!.items!!.toMutableList().apply {
                    removeAt(index)
                    add(
                        index,
                        SpaceItem(
                            SpaceContent.Text(page = content.page, text),
                            item.position
                        )
                    )
                }
            )
            dirty = nextInt()
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
