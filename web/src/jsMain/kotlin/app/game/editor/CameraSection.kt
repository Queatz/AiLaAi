package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import components.IconButton
import format1Decimal
import format3Decimals
import kotlin.math.round
import game.CameraKeyframe
import game.Map
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
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import r

@Composable
fun CameraSection(map: Map?) {
    PanelSection(
        title = "Camera",
        icon = "camera",
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        if (map == null) {
            Text("No map loaded")
            return@PanelSection
        }

        val game = map.game
        if (game == null) {
            Text("No game loaded")
            return@PanelSection
        }

        // FOV controls
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            // FOV text input and slider (0.25-2.0)
            // Initialize with the current camera FOV or default to 0.5f if not available
            var fovValue by remember { mutableStateOf(map.game?.scene?.activeCamera?.fov ?: 0.5f) }
            LaunchedEffect(game.animationData.currentTime) {
                game.scene.activeCamera?.fov?.let { fov ->
                    fovValue = fov
                }
            }

            Text("Field of View")

            TextInput(fovValue.format3Decimals()) {
                classes(Styles.textarea)
                placeholder("Field of view (0.25 – 2.0)")
                style {
                    width(100.percent)
                    marginBottom(0.5.r)
                }
                onInput { event ->
                    val fov = event.value.toFloatOrNull()
                    if (fov != null) {
                        val clampedFov = minOf(2.0f, maxOf(0.25f, fov))
                        fovValue = clampedFov
                        // Set the FOV in the map
                        map.set("fov", clampedFov)
                    }
                }
            }

            // FOV slider (0.25-2.0)
            RangeInput(
                fovValue,
                min = 0.25,
                max = 2.0,
                step = 0.01
            ) {
                style {
                    width(100.percent)
                }
                onInput {
                    val fov = it.value!!.toFloat()
                    fovValue = fov
                    // Set the FOV in the map
                    map.set("fov", fov)
                }
            }
        }

        // Camera keyframes section
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(0.5.r)
                marginBottom(1.r)
            }
        }) {
            Text("Camera Keyframes")

            // Button to add camera keyframe at current position
            Button({
                classes(Styles.button)
                style {
                    marginBottom(0.5.r)
                }
                onClick {
                    game.animationData.addCameraKeyframe(map.camera)
                }
            }) {
                Text("Save camera position at current frame")
            }

            // List all camera keyframes
            if (game.animationData.cameraKeyframes.isEmpty()) {
                Div { Text("No camera keyframes added yet") }
            } else {
                // List all keyframes
                game.animationData.cameraKeyframes.forEach { keyframe ->
                    CameraKeyframeItem(game, keyframe)
                }
            }
        }
    }
}

@Composable
private fun CameraKeyframeItem(game: game.Game, keyframe: CameraKeyframe) {
    var isEditing by remember { mutableStateOf(false) }
    var editTime by remember(keyframe) { mutableStateOf(keyframe.time.toString()) }
    var editFov by remember(keyframe) { mutableStateOf(keyframe.fov) }

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
                // Input for keyframe time
                TextInput(editTime) {
                    classes(Styles.textarea)
                    placeholder("Time (seconds)")
                    style {
                        width(100.percent)
                    }
                    onInput { event ->
                        editTime = event.value
                    }
                }
            }
            // FOV input for keyframe
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(0.5.r)
                    marginBottom(0.5.r)
                }
            }) {
                Text("Field of View")
                TextInput(editFov.format3Decimals()) {
                    classes(Styles.textarea)
                    placeholder("Field of view (0.25 – 2.0)")
                    style {
                        width(100.percent)
                        marginBottom(0.5.r)
                    }
                    onInput { event ->
                        val f = event.value.toFloatOrNull()
                        if (f != null) {
                            editFov = f.coerceIn(0.25f, 2f)
                        }
                    }
                }
                RangeInput(
                    editFov,
                    min = 0.25,
                    max = 2.0,
                    step = 0.01
                ) {
                    style {
                        width(100.percent)
                    }
                    onInput {
                        editFov = it.value!!.toFloat()
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
                        editTime = keyframe.time.toString()
                    }
                }) {
                    Text("Cancel")
                }

                // Save button
                Button({
                    classes(Styles.button)
                    onClick { 
                        // Clip time to 3 decimal places
                        keyframe.time = editTime.toDoubleOrNull()?.let {
                            (round(it * 1000.0) / 1000.0)
                        } ?: keyframe.time
                        // Update keyframe FOV
                        keyframe.fov = editFov.coerceIn(0.25f, 2f)
                        // Force update of camera keyframes list to trigger UI recomposition
                        game.animationData.updateCameraKeyframes()
                        // Update seekbar right away
                        game.setTime(game.animationData.currentTime)
                        isEditing = false
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
                    // todo translate
                    Text("${keyframe.time.format3Decimals()} seconds")
                }

                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(0.5.r)
                    }
                }) {
                    IconButton(
                        name = "edit",
                        title = "Edit keyframe",
                        onClick = {
                            isEditing = true
                        }
                    )

                    IconButton(
                        name = "delete",
                        title = "Delete keyframe",
                        onClick = {
                            game.animationData.removeCameraKeyframe(keyframe.id)
                        }
                    )

                    IconButton(
                        name = "play_arrow",
                        title = "Go to keyframe",
                        onClick = {
                            game.setTime(keyframe.time)
                        }
                    )

                    // Apply camera position without changing time
                    IconButton(
                        name = "camera_alt",
                        title = "Apply camera position without changing time",
                        onClick = {
                            game.animationData.applyCameraKeyframeById(game.map.camera, keyframe.id)
                        }
                    )
                }
            }
        }
    }
}
