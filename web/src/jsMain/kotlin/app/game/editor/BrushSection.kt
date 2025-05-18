package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import game.Map
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
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
            // Brush size input (1-100)
            var brushSize by remember { mutableStateOf(1) }

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

            // Brush density input (1-100)
            var brushDensity by remember { mutableStateOf(100.0) }

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

            // Grid size input (10-100)
            var gridSize by remember { mutableStateOf(50) }

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
                }
                onInput {
                    val size = it.value!!.toInt()
                    gridSize = size
                    // Add 1 to the value for the actual implementation (11-101)
                    map?.set("gridSize", size + 1)
                }
            }
        }
    }
}
