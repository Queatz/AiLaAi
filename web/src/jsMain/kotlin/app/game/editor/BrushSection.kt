package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import game.Map
import lib.Color4
import notBlank
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import r

@Composable
fun BrushSection(map: Map? = null) {
    PanelSection(
        title = "Brush",
        icon = "brush",
        initiallyExpanded = true,
        closeOtherPanels = true
    ) {
        Div({
            style {
                padding(.5.r)
            }
        }) {
            // If sketch tool is active, show sketch color and thickness
            if (map?.isSketching == true) {
                // Color picker
                // Initialize color picker with the current sketch color
                val initColor = map.sketchManager.currentColor
                var colorHex by remember { mutableStateOf(
                    "#" + listOf(initColor.r, initColor.g, initColor.b)
                        .map { (it * 255).toInt().coerceIn(0,255).toString(16).padStart(2,'0') }
                        .joinToString("")
                ) }
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        marginBottom(0.5.r)
                    }
                }) {
                    Div { Text("Color") }
                    Input(type = InputType.Color) {
                        attr("value", colorHex)
                        onInput { event ->
                            val hex = event.value.notBlank ?: "#000000"
                            colorHex = hex
                            val hexStr = hex.removePrefix("#")
                            if (hexStr.length >= 6) {
                                val r = hexStr.substring(0, 2).toInt(16) / 255f
                                val g = hexStr.substring(2, 4).toInt(16) / 255f
                                val b = hexStr.substring(4, 6).toInt(16) / 255f
                                map.sketchManager.currentColor = Color4(r, g, b, 1f)
                            }
                        }
                    }
                }
                // Thickness slider group
                var thickness by remember { mutableStateOf(map.sketchManager.currentThickness) }
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        marginBottom(1.r)
                    }
                }) {
                    Div { Text("Thickness") }
                    RangeInput(
                        value = thickness.toDouble(),
                        min = 1.0,
                        max = 10.0,
                        step = 1.0
                    ) {
                        onInput { event ->
                            val t = event.value!!.toInt()
                            thickness = t
                            map.sketchManager.currentThickness = t
                        }
                    }
                }
                return@Div
            }
            // Brush size input (1-100), initialize from map state when available
            var brushSize by remember(map) { mutableStateOf(map?.tilemapEditor?.brushSize ?: 1) }

            Text("Brush Size")

            TextInput(brushSize.toString()) {
                classes(Styles.textarea)
                placeholder("Size (1-10)")
                style {
                    width(100.percent)
                    marginBottom(0.5.r)
                }
                onInput { event ->
                    val size = event.value.toIntOrNull()
                    if (size != null && map != null) {
                        val clampedSize = size.coerceIn(1, 10)
                        brushSize = clampedSize
                        // Set the brush size in the map
                        map.set("brushSize", clampedSize)
                    }
                }
            }

            // Brush size slider (1-10)
            RangeInput(
                value = brushSize.toDouble(),
                min = 1.0,
                max = 10.0,
                step = 1.0
            ) {
                style {
                    width(100.percent)
                    marginBottom(1.r)
                }
                onInput {
                    val size = it.value!!.toInt()
                    brushSize = size
                    map?.set("brushSize", size)
                }
            }

            // Brush density input (0.1-100), initialize from map state when available
            var brushDensity by remember(map) { mutableStateOf(map?.tilemapEditor?.brushDensity?.toDouble() ?: 100.0) }

            Text("Brush Density")

            TextInput(brushDensity.toString()) {
                classes(Styles.textarea)
                placeholder("Density (0.1-100)")
                style {
                    width(100.percent)
                    marginBottom(0.5.r)
                }
                onInput { event ->
                    val density = event.value.toDoubleOrNull()
                    if (density != null && map != null) {
                        val clampedDensity = density.coerceIn(.1, 100.0)
                        brushDensity = clampedDensity
                        // Set the brush density in the map
                        map.set("brushDensity", clampedDensity)
                    }
                }
            }

            // Brush density slider (1-100)
            RangeInput(
                value = brushDensity,
                min = .1,
                max = 100.0,
                step = .1
            ) {
                style {
                    width(100.percent)
                    marginBottom(1.r)
                }
                onInput {
                    val density = it.value!!.toDouble()
                    brushDensity = density
                    map?.set("brushDensity", density)
                }
            }

            // Grid size input (10-100), initialize from map state when available (map.gridSize is 11-101)
            var gridSize by remember(map) {
                // subtract 1 to convert map.gridSize (11-101) to slider value (10-100)
                val initial = map?.tilemapEditor?.gridSize?.minus(1) ?: 50
                mutableStateOf(initial)
            }

            Text("Grid Size")

            // Grid size slider (10-100 in steps of 5)
            RangeInput(
                gridSize.toDouble(),
                min = 10.0,
                max = 100.0,
                step = 5.0
            ) {
                style {
                    width(100.percent)
                    marginBottom(1.r)
                }
                onInput {
                    val size = it.value!!.toInt()
                    gridSize = size
                    // Add 1 to the value for the actual implementation (11-101)
                    map?.set("gridSize", size + 1)
                }
            }

            // Grid line width input (1-10), initialize from map state when available
            var gridLineAlpha by remember(map) {
                val initial = map?.tilemapEditor?.gridLineAlpha ?: 1
                mutableStateOf(initial)
            }

            Text("Grid Alpha")

            // Grid line width slider (1-10)
            RangeInput(
                gridLineAlpha.toDouble(),
                min = 1.0,
                max = 10.0,
                step = 1.0
            ) {
                style {
                    width(100.percent)
                }
                onInput {
                    val width = it.value!!.toInt()
                    gridLineAlpha = width
                    map?.set("gridLineAlpha", width)
                }
            }
        }
    }
}
