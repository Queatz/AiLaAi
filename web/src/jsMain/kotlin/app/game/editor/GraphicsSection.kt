package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import components.LabeledSwitch
import game.Map
import lib.Engine
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.TextInput
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.RenderingContext
import r
import web.cssom.atrule.width

@Composable
fun GraphicsSection(
    engine: Engine? = null,
    map: Map? = null,
    onPixelatedChanged: (Boolean) -> Unit = {},
    initialPixelSize: Int? = null,
    onPixelSizeChanged: (Int) -> Unit = {},
) {
    PanelSection(
        title = "Graphics",
        icon = "settings",
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        Div({
            style {
                padding(.5.r)
            }
        }) {
            // Pixel size input (1-16)
            var pixelSize by remember { mutableStateOf(initialPixelSize?.toString() ?: "1") }

            // Initialize hardware scaling level if initialPixelSize is provided
            LaunchedEffect(initialPixelSize) {
                initialPixelSize?.let { size ->
                    if (engine != null) {
                        val clampedSize = minOf(16, maxOf(1, size))
                        engine.setHardwareScalingLevel(clampedSize)
                        pixelSize = clampedSize.toString()
                    }
                }
            }

            TextInput(pixelSize) {
            classes(Styles.textarea)
                placeholder("Pixel size (1-16)")
                style {
                    width(100.percent)
                }
                onInput { event ->
                    val size = event.value.toIntOrNull()
                    if (size != null && engine != null) {
                        val clampedSize = minOf(16, maxOf(1, size))
                        pixelSize = clampedSize.toString()
                        // Set the hardware scaling level
                        engine.setHardwareScalingLevel(clampedSize)
                        // Notify about the change
                        onPixelSizeChanged(clampedSize)
                    } else {
                        pixelSize = event.value
                    }
                }
            }

            // Post-processing effects checkboxes
            if (map != null) {
                Div({
                    style {
                        padding(.5.r, 0.r, 0.r, 0.r)
                    }
                }) {
                    // SSAO checkbox
                    var ssaoEnabled by remember { mutableStateOf<Boolean>(map.post.ssaoEnabled) }
                    Div {
                        LabeledSwitch(
                            value = ssaoEnabled,
                            onValue = { 
                                ssaoEnabled = it
                                map.post.ssaoEnabled = it
                            },
                            onChange = { 
                                ssaoEnabled = it
                                map.post.ssaoEnabled = it
                            },
                            title = "SSAO"
                        )
                    }

                    Div({ style { padding(1.r, 0.r, 0.r, 0.r) } }) {}

                    // Bloom checkbox
                    var bloomEnabled by remember { mutableStateOf<Boolean>(map.post.bloomEnabled) }
                    Div {
                        LabeledSwitch(
                            value = bloomEnabled,
                            onValue = { 
                                bloomEnabled = it
                                map.post.bloomEnabled = it
                            },
                            onChange = { 
                                bloomEnabled = it
                                map.post.bloomEnabled = it
                            },
                            title = "Bloom"
                        )
                    }

                    Div({ style { padding(1.r, 0.r, 0.r, 0.r) } }) {}

                    // Sharpen checkbox
                    var sharpenEnabled by remember { mutableStateOf<Boolean>(map.post.sharpenEnabled) }
                    Div {
                        LabeledSwitch(
                            value = sharpenEnabled,
                            onValue = { 
                                sharpenEnabled = it
                                map.post.sharpenEnabled = it
                            },
                            onChange = { 
                                sharpenEnabled = it
                                map.post.sharpenEnabled = it
                            },
                            title = "Sharpen"
                        )
                    }

                    Div({ style { padding(1.r, 0.r, 0.r, 0.r) } }) {}

                    // Color Correction checkbox
                    var colorCorrectionEnabled by remember { mutableStateOf<Boolean>(map.post.colorCorrectionEnabled) }
                    Div {
                        LabeledSwitch(
                            value = colorCorrectionEnabled,
                            onValue = { 
                                colorCorrectionEnabled = it
                                map.post.colorCorrectionEnabled = it
                            },
                            onChange = { 
                                colorCorrectionEnabled = it
                                map.post.colorCorrectionEnabled = it
                            },
                            title = "Color Correction"
                        )
                    }

                    Div({ style { padding(1.r, 0.r, 0.r, 0.r) } }) {}

                    // Depth of Field checkbox
                    var depthOfFieldEnabled by remember { mutableStateOf<Boolean>(map.post.depthOfFieldEnabled) }
                    Div {
                        LabeledSwitch(
                            value = depthOfFieldEnabled,
                            onValue = { 
                                depthOfFieldEnabled = it
                                map.post.depthOfFieldEnabled = it
                            },
                            onChange = { 
                                depthOfFieldEnabled = it
                                map.post.depthOfFieldEnabled = it
                            },
                            title = "Depth of Field"
                        )
                    }

                    Div({ style { padding(1.r, 0.r, 0.r, 0.r) } }) {}

                    // Linear Sampling checkbox
                    var linearSamplingEnabled by remember { mutableStateOf(map.linearSamplingEnabled) }

                    LaunchedEffect(linearSamplingEnabled) {
                        onPixelatedChanged(linearSamplingEnabled)
                    }

                    Div {
                        LabeledSwitch(
                            value = linearSamplingEnabled,
                            onValue = { 
                                linearSamplingEnabled = it
                                map.linearSamplingEnabled = it
                            },
                            onChange = { 
                                linearSamplingEnabled = it
                                map.linearSamplingEnabled = it
                            },
                            title = "Pixel Art"
                        )
                    }
                }
            }
        }
    }
}
