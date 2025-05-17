package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import components.IconButton
import format1Decimal
import game.AnimationMarker
import game.Game
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import r

@Composable
fun AnimationSection(game: Game?) {
    PanelSection(
        title = "Animation",
        icon = "animation",
        enabled = game != null,
        initiallyExpanded = false
    ) {
        if (game == null) {
            Text("No game loaded")
            return@PanelSection
        }

        // Remember playing state and update it when game state changes
        var isPlaying by remember { mutableStateOf(game.isPlaying()) }

        // Set up flow collection to update our state when play state changes
        LaunchedEffect(game) {
            game.playStateFlow.collectLatest { playing ->
                isPlaying = playing
            }
        }

        // For backward compatibility, still handle the callback
        DisposableEffect(game) {
            val originalCallback = game.onPlayStateChanged

            game.onPlayStateChanged = { playing ->
                isPlaying = playing
                originalCallback?.invoke(playing)
            }

            onDispose {
                game.onPlayStateChanged = originalCallback
            }
        }

        // Animation controls
        Div({
            style {
                display(DisplayStyle.Flex)
                gap(0.5.r)
                marginBottom(1.r)
            }
        }) {
            // Jump to start button
            IconButton(
                name = "first_page",
                title = "Jump to start (reset to 0)",
                background = true,
                onClick = {
                    game.setTime(0.0)
                }
            )

            // Play/Pause button
            IconButton(
                name = if (isPlaying) "pause" else "play_arrow",
                title = if (isPlaying) "Pause animation" else "Play animation",
                background = true,
                onClick = {
                    game.togglePlayback()
                }
            )

            // Jump to end button
            IconButton(
                name = "last_page",
                title = "Jump to end",
                background = true,
                onClick = {
                    game.setTime(game.animationData.totalDuration)
                }
            )
        }

        // State for new marker name
        var newMarkerName by remember { mutableStateOf("") }

        // Add marker section
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(0.5.r)
                marginBottom(1.r)
            }
        }) {
            Text("Add Marker")

            Div({
                style {
                    display(DisplayStyle.Flex)
                    gap(0.5.r)
                    alignItems(AlignItems.Center)
                }
            }) {
                // Input for marker name
                TextInput(newMarkerName) {
                    classes(Styles.textarea)
                    placeholder("Marker name")
                    style {
                        width(100.percent)
                        marginRight(0.5.r)
                    }
                    onInput { event ->
                        newMarkerName = event.value
                    }
                }

                // Button to add marker
                Button({
                    classes(Styles.button)
                    onClick { 
                        if (newMarkerName.isNotBlank()) {
                            game.animationData.addMarker(newMarkerName)
                            newMarkerName = ""
                        }
                    }
                }) {
                    Text("Add")
                }
            }
        }

        // Markers list
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(0.5.r)
            }
        }) {
            Text("Markers")

            if (game.animationData.markers.isEmpty()) {
                Div { Text("No markers added yet") }
            } else {
                // List all markers
                game.animationData.markers.forEach { marker ->
                    MarkerItem(game, marker)
                }
            }
        }
    }
}

@Composable
private fun MarkerItem(game: Game, marker: AnimationMarker) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(marker.name) }
    var editTime by remember { mutableStateOf(marker.time.toString()) }
    var editDuration by remember { mutableStateOf(marker.duration.toString()) }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            padding(0.5.r)
            marginBottom(0.5.r)
            property("background-color", "rgba(0, 0, 0, 0.05)")
            property("border-radius", "4px")
        }
    }) {
        if (isEditing) {
            // Edit mode
            Div({
                style {
                    display(DisplayStyle.Flex)
                    gap(0.5.r)
                    marginBottom(0.5.r)
                }
            }) {
                // Input for marker name
                TextInput(editName) {
                    classes(Styles.textarea)
                    placeholder("Marker name")
                    style {
                        width(100.percent)
                        marginRight(0.5.r)
                    }
                    onInput { event ->
                        editName = event.value
                    }
                }

                // Input for marker time
                TextInput(editTime) {
                    classes(Styles.textarea)
                    placeholder("Time (seconds)")
                    style {
                        width(100.percent)
                        marginBottom(0.5.r)
                    }
                    onInput { event ->
                        editTime = event.value
                    }
                }

                // Input for marker duration
                TextInput(editDuration) {
                    classes(Styles.textarea)
                    placeholder("Duration (seconds, 0 = play until end)")
                    style {
                        width(100.percent)
                    }
                    onInput { event ->
                        editDuration = event.value
                    }
                }
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    gap(0.5.r)
                    justifyContent(JustifyContent.FlexEnd)
                }
            }) {
                // Cancel button
                Button({
                    classes(Styles.textButton)
                    onClick { 
                        isEditing = false
                        editName = marker.name
                        editTime = marker.time.toString()
                    }
                }) {
                    Text("Cancel")
                }

                // Save button
                Button({
                    classes(Styles.button)
                    onClick { 
                        if (editName.isNotBlank()) {
                            marker.name = editName
                            marker.time = editTime.toDoubleOrNull() ?: marker.time
                            marker.duration = editDuration.toDoubleOrNull() ?: marker.duration
                            // Update seekbar right away when changing marker duration
                            game.setTime(game.animationData.currentTime)
                            isEditing = false
                        }
                    }
                }) {
                    Text("Save")
                }
            }
        } else {
            // View mode
            Div({
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.SpaceBetween)
                    alignItems(AlignItems.Center)
                }
            }) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                    }
                }) {
                    Span({
                        style {
                            property("font-weight", "bold")
                        }
                    }) {
                        Text(marker.name)
                    }
                    Text("${marker.time.format1Decimal()} ${if (marker.duration > 0) marker.duration.format1Decimal() else ""}")
                }

                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(0.5.r)
                    }
                }) {
                    IconButton(
                        name = "edit",
                        title = "Edit marker",
                        onClick = {
                            isEditing = true
                        }
                    )

                    IconButton(
                        name = "delete",
                        title = "Delete marker",
                        onClick = {
                            game.animationData.removeMarker(marker.id)
                        }
                    )

                    IconButton(
                        name = "play_arrow",
                        title = "Go to marker",
                        onClick = {
                            game.setTime(marker.time)
                        }
                    )
                }
            }
        }
    }
}
