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
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
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
                Div({
                    style {
                        padding(.5.r)
                        marginTop(.5.r)
                        marginBottom(.5.r)
                    }
                }) {
                    // Use updateTimestamp to force recomposition when the clone state changes
                    // This is a dummy usage that doesn't affect the UI but ensures recomposition
                    @Suppress("UNUSED_VARIABLE")
                    val dummy = updateTimestamp

                    val instructionText = when (cloneState) {
                        TilemapEditor.CloneSelectionState.NotStarted ->
                            "Step 1/4: Click to select the first corner of your selection"

                        TilemapEditor.CloneSelectionState.FirstPointSelected ->
                            "Step 2/4: Click to select the second corner (width/depth)"

                        TilemapEditor.CloneSelectionState.SecondPointSelected ->
                            "Step 3/4: Click to set the height of your selection"

                        TilemapEditor.CloneSelectionState.Complete ->
                            "Step 4/4: Click to place your cloned selection. Alt+click to erase."

                        else -> ""
                    }

                    Text(instructionText)
                }

                if (cloneState != TilemapEditor.CloneSelectionState.NotStarted) {
                    // Reset clone selection button
                    Button({
                        classes(Styles.outlineButton)
                        style {
                            marginRight(0.5.r)
                            marginBottom(0.5.r)
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
