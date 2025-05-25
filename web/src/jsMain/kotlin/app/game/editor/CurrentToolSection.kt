package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import game.DrawMode
import game.Map
import game.TilemapEditor
import game.ToolType
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.Date
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.KeyboardEvent
import r

@Composable
fun CurrentToolSection(
    map: Map,
    onToolDeselected: () -> Unit = {}
) {
    PanelSection(
        title = "Tool",
        icon = "build",
        initiallyExpanded = true,
        closeOtherPanels = true
    ) {
        // Use the toolState for draw mode and selected tool
        // This ensures we have a single source of truth
        var currentDrawMode by remember { mutableStateOf(map.toolState.drawMode) }
        var selectedToolType by remember { mutableStateOf<ToolType?>(
            when (currentDrawMode) {
                DrawMode.Tile, DrawMode.Object -> ToolType.DRAW
                DrawMode.Clone -> ToolType.CLONE
            }
        ) }

        // Use a state variable for the clone selection state so it can be updated
        var cloneState by remember { mutableStateOf<TilemapEditor.CloneSelectionState?>(null) }

        // Use a timestamp to force recomposition
        var updateTimestamp by remember { mutableStateOf(Date.now()) }

        // Initial update of the clone state
        LaunchedEffect(currentDrawMode) {
            cloneState = if (currentDrawMode == DrawMode.Clone) {
                map.tilemapEditor.getCloneSelectionState()
            } else {
                null
            }
        }

        // Set up an interval to periodically check for changes in the clone selection state
        DisposableEffect(currentDrawMode) {
            var intervalId: Int? = null

            // Only set up the interval if we're in clone mode
            if (currentDrawMode == DrawMode.Clone) {
                intervalId = window.setInterval({
                    // Get the current clone state
                    val currentState = map.tilemapEditor.getCloneSelectionState()

                    // Update the state if it has changed
                    if (currentState != cloneState) {
                        cloneState = currentState
                        // Force recomposition by updating the timestamp
                        updateTimestamp = Date.now()
                    }
                }, 100) // Check every 100ms
            }

            // Clean up the interval when the component is disposed or the draw mode changes
            onDispose {
                intervalId?.let { window.clearInterval(it) }
            }
        }

        // Set up keyboard event listeners for number keys 1, 2, 3
        DisposableEffect(Unit) {
            // Function to handle key press events
            val keydownListener = object : EventListener {
                override fun handleEvent(event: Event) {
                    // Cast to KeyboardEvent
                    val keyEvent = event as? KeyboardEvent ?: return

                    // Only process if not in an input field
                    val target = keyEvent.target
                    val isInputField = target is HTMLInputElement ||
                                      target is HTMLTextAreaElement

                    if (!isInputField) {
                        when (keyEvent.key) {
                            "1" -> {
                                // Toggle Draw tool
                                if (selectedToolType == ToolType.DRAW) {
                                    // Deselect current tool
                                    selectedToolType = null
                                    map.toolState.selectTool(null as ToolType?)
                                    currentDrawMode = map.toolState.drawMode
                                    map.camera.camera.attachControl()
                                    onToolDeselected()
                                } else {
                                    // Select Draw tool
                                    selectedToolType = ToolType.DRAW
                                    map.toolState.selectTool(ToolType.DRAW)
                                    currentDrawMode = map.toolState.drawMode
                                }
                                keyEvent.preventDefault()
                            }
                            "2" -> {
                                // Toggle Clone tool
                                if (selectedToolType == ToolType.CLONE) {
                                    // Deselect current tool
                                    selectedToolType = null
                                    map.toolState.selectTool(null as ToolType?)
                                    currentDrawMode = map.toolState.drawMode
                                    map.camera.camera.attachControl()
                                    onToolDeselected()
                                } else {
                                    // Select Clone tool
                                    selectedToolType = ToolType.CLONE
                                    map.toolState.selectTool(ToolType.CLONE)
                                    currentDrawMode = map.toolState.drawMode
                                }
                                keyEvent.preventDefault()
                            }
                            "3" -> {
                                // Toggle Sketch tool
                                if (selectedToolType == ToolType.SKETCH) {
                                    // Deselect current tool
                                    selectedToolType = null
                                    map.toolState.selectTool(null as ToolType?)
                                    currentDrawMode = map.toolState.drawMode
                                    map.camera.camera.attachControl()
                                    onToolDeselected()
                                } else {
                                    // Select Sketch tool
                                    selectedToolType = ToolType.SKETCH
                                    map.toolState.selectTool(ToolType.SKETCH)
                                    currentDrawMode = map.toolState.drawMode
                                    // Detach camera control while sketching
                                    map.camera.camera.detachControl()
                                }
                                keyEvent.preventDefault()
                            }
                            "4" -> {
                                // Toggle Bucket tool
                                if (selectedToolType == ToolType.BUCKET) {
                                    // Deselect current tool
                                    selectedToolType = null
                                    map.toolState.selectTool(null as ToolType?)
                                    currentDrawMode = map.toolState.drawMode
                                    map.camera.camera.attachControl()
                                    onToolDeselected()
                                } else {
                                    // Select Bucket tool
                                    selectedToolType = ToolType.BUCKET
                                    map.toolState.selectTool(ToolType.BUCKET)
                                    currentDrawMode = map.toolState.drawMode
                                }
                                keyEvent.preventDefault()
                            }
                            "5" -> {
                                // Toggle Line tool
                                if (selectedToolType == ToolType.LINE) {
                                    // Deselect current tool
                                    selectedToolType = null
                                    map.toolState.selectTool(null as ToolType?)
                                    currentDrawMode = map.toolState.drawMode
                                    map.camera.camera.attachControl()
                                    onToolDeselected()
                                } else {
                                    // Select Line tool
                                    selectedToolType = ToolType.LINE
                                    map.toolState.selectTool(ToolType.LINE)
                                    currentDrawMode = map.toolState.drawMode
                                }
                                keyEvent.preventDefault()
                            }
                            "Escape", "Esc" -> {
                                if (selectedToolType == ToolType.LINE) {
                                    // Cancel line drawing if active
                                    map.tilemapEditor.cancelLineDrawing()
                                    keyEvent.preventDefault()
                                }
                            }
                        }
                    }
                }
            }

            // Add event listener to document
            kotlinx.browser.document.addEventListener("keydown", keydownListener)

            // Clean up event listener when component is disposed
            onDispose {
                kotlinx.browser.document.removeEventListener("keydown", keydownListener)
            }
        }


        Div({
            style {
                padding(.5.r)
            }
        }) {
            // Create tool list for ToolGrid
            val tools = listOf(
                Tool(
                    id = ToolType.DRAW,
                    name = "Draw",
                    photoUrl = "/assets/icons/brush.svg",
                    description = "Draw tiles and objects",
                    number = 1
                ),
                Tool(
                    id = ToolType.CLONE,
                    name = "Clone",
                    photoUrl = "/assets/icons/select.svg",
                    description = "Clone a selection of tiles",
                    number = 2
                ),
                Tool(
                    id = ToolType.SKETCH,
                    name = "Sketch",
                    photoUrl = "/assets/icons/sketch.svg",
                    description = "Draw freehand sketches",
                    number = 3
                ),
                Tool(
                    id = ToolType.BUCKET,
                    name = "Bucket",
                    photoUrl = "/assets/icons/format_color_fill.svg",
                    description = "Fill connected tiles of the same type",
                    number = 4
                ),
                Tool(
                    id = ToolType.LINE,
                    name = "Line",
                    photoUrl = "/assets/icons/timeline.svg",
                    description = "Draw lines of tiles",
                    number = 5
                )
            )

            // Use ToolGrid for tool selection with toggle-to-deselect behavior
            ToolGrid(
                tools = tools,
                selectedToolType = selectedToolType,
                onToolSelected = { tool ->
                    if (tool.id == selectedToolType) {
                        // Deselect current tool
                        selectedToolType = null
                        map.toolState.selectTool(null as ToolType?)
                        currentDrawMode = map.toolState.drawMode
                        // Re-enable camera and show cursor
                        map.camera.camera.attachControl()
                        map.tilemapEditor.cursor.isVisible = true
                        onToolDeselected()
                    } else {
                        // Select new tool
                        selectedToolType = tool.id
                        map.toolState.selectTool(tool.id)
                        currentDrawMode = map.toolState.drawMode

                        // Handle camera control based on tool
                        if (tool.id == ToolType.SKETCH) {
                            // Detach camera control while sketching
                            map.camera.camera.detachControl()
                            // Cursor visibility is handled in TilemapEditor.update() based on toolState.isSketching
                        }
                    }
                }
            )

            // Show clone instructions and current step if in clone mode
            if (currentDrawMode == DrawMode.Clone) {
                // Use updateTimestamp to force recomposition when the clone state changes
                // This is a dummy usage that doesn't affect the UI but ensures recomposition
                @Suppress("UNUSED_VARIABLE")
                val dummy = updateTimestamp

                val currentStep = when (cloneState) {
                    TilemapEditor.CloneSelectionState.NotStarted -> 1
                    TilemapEditor.CloneSelectionState.FirstPointSelected -> 2
                    TilemapEditor.CloneSelectionState.SecondPointSelected -> 3
                    TilemapEditor.CloneSelectionState.Complete -> 4
                    else -> 1
                }

                // Step indicator with 4 sections
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        justifyContent(JustifyContent.SpaceBetween)
                        marginTop(1.r)
                        marginBottom(1.r)
                    }
                }) {
                    // Create 4 step indicators
                    for (step in 1..4) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                alignItems(AlignItems.Center)
                                gap(0.5.r)
                                width(5.r)
                            }
                        }) {
                            // Step circle
                            Div({
                                style {
                                    width(2.r)
                                    height(2.r)
                                    borderRadius(1.r)
                                    backgroundColor(if (step == currentStep) Styles.colors.primary else Color("#e0e0e0"))
                                    display(DisplayStyle.Flex)
                                    justifyContent(JustifyContent.Center)
                                    alignItems(AlignItems.Center)
                                    color(if (step == currentStep) Color.white else Color("#666666"))
                                    fontWeight(if (step == currentStep) "bold" else "normal")
                                }
                            }) {
                                Text("$step")
                            }

                            // Step label
                            Div({
                                style {
                                    fontSize(12.px)
                                    textAlign("center")
                                    color(if (step == currentStep) Styles.colors.primary else Color("#666666"))
                                    fontWeight(if (step == currentStep) "bold" else "normal")
                                }
                            }) {
                                Text(
                                    when (step) {
                                        1 -> "First Corner"
                                        2 -> "Second Corner"
                                        3 -> "Height"
                                        4 -> "Draw!"
                                        else -> ""
                                    }
                                )
                            }
                        }
                    }
                }

                // Instruction box with rounded corners and primary color
                Div({
                    style {
                        padding(1.r)
                        marginTop(.5.r)
                        marginBottom(1.r)
                        borderRadius(1.r)
                        border(1.px, LineStyle.Solid, Styles.colors.primary)
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(0.5.r)
                    }
                }) {
                    // Add an icon based on the current step
                    Div({
                        classes("material-symbols-outlined")
                        style {
                            color(Styles.colors.primary)
                            fontSize(24.px)
                        }
                    }) {
                        Text(when (cloneState) {
                            TilemapEditor.CloneSelectionState.NotStarted -> "touch_app"
                            TilemapEditor.CloneSelectionState.FirstPointSelected -> "open_with"
                            TilemapEditor.CloneSelectionState.SecondPointSelected -> "height"
                            TilemapEditor.CloneSelectionState.Complete -> "content_copy"
                            else -> "help"
                        })
                    }

                    val instructionText = when (cloneState) {
                        TilemapEditor.CloneSelectionState.NotStarted ->
                            "Click to select the first corner of your selection"

                        TilemapEditor.CloneSelectionState.FirstPointSelected ->
                            "Click to select the second corner (width/depth)"

                        TilemapEditor.CloneSelectionState.SecondPointSelected ->
                            "Click to set the height of your selection"

                        TilemapEditor.CloneSelectionState.Complete ->
                            "Click to place your cloned selection. Alt+click to erase."

                        else -> ""
                    }

                    Text(instructionText)
                }

                if (cloneState != TilemapEditor.CloneSelectionState.NotStarted) {
                    // Reset clone selection button
                    Button({
                        classes(Styles.button)
                        style {
                            marginRight(0.5.r)
                            marginBottom(0.5.r)
                            width(100.percent)
                        }
                        onClick {
                            // Reset the clone selection
                            map.tilemapEditor.resetCloneSelection()
                        }
                    }) {
                        Text("Reset Selection")
                    }
                }
            }
        }
    }
}
