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
import kotlinx.browser.window
import kotlin.js.Date
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun CurrentToolSection(map: Map) {
    PanelSection(
        title = "Tool",
        icon = "build",
        initiallyExpanded = true,
        closeOtherPanels = true
    ) {
        var currentDrawMode by remember { mutableStateOf(map.tilemapEditor.drawMode) }

        // Use a state variable for the clone selection state so it can be updated
        var cloneState by remember { mutableStateOf<TilemapEditor.CloneSelectionState?>(null) }

        // Use a timestamp to force recomposition
        var updateTimestamp by remember { mutableStateOf(Date.now()) }

        // Initial update of the clone state
        LaunchedEffect(currentDrawMode) {
            if (currentDrawMode == DrawMode.Clone) {
                cloneState = map.tilemapEditor.getCloneSelectionState()
            } else {
                cloneState = null
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

        Div({
            style {
                padding(.5.r)
            }
        }) {
            // Create tool list for ToolGrid
            val tools = listOf(
                Tool(
                    id = "draw",
                    name = "Draw",
                    photoUrl = "/assets/icons/brush.svg",
                    description = "Draw tiles and objects"
                ),
                Tool(
                    id = "clone",
                    name = "Clone",
                    photoUrl = "/assets/icons/select.svg",
                    description = "Clone a selection of tiles"
                )
            )

            // Determine selected tool ID based on current draw mode
            val selectedToolId = when (currentDrawMode) {
                DrawMode.Tile, DrawMode.Object -> "draw"
                DrawMode.Clone -> "clone"
            }

            // Use ToolGrid for tool selection
            ToolGrid(
                tools = tools,
                selectedToolId = selectedToolId,
                onToolSelected = { tool ->
                    when (tool.id) {
                        "draw" -> {
                            map.tilemapEditor.drawMode = DrawMode.Tile
                            currentDrawMode = DrawMode.Tile
                        }

                        "clone" -> {
                            map.tilemapEditor.drawMode = DrawMode.Clone
                            currentDrawMode = DrawMode.Clone
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
