package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import game.Map
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun EnvironmentSection(map: Map? = null) {
    PanelSection(
        title = "Environment",
        icon = "landscape",
        enabled = map != null,
        initiallyExpanded = true
    ) {
        if (map == null) {
            Text("No map loaded")
            return@PanelSection
        }

        // Background Color controls
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            Text("Background Color")

            // Red component slider
            var redValue by remember { mutableStateOf(map.getBackgroundColorR()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                Text("Red")
                RangeInput(
                    redValue,
                    min = 0.0,
                    max = 1.0,
                    step = 0.01
                ) {
                    style {
                        width(100.percent)
                    }
                    onInput {
                        val value = it.value!!.toFloat()
                        redValue = value
                        map.set("backgroundColorR", value)
                    }
                }
            }

            // Green component slider
            var greenValue by remember { mutableStateOf(map.getBackgroundColorG()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                Text("Green")
                RangeInput(
                    greenValue,
                    min = 0.0,
                    max = 1.0,
                    step = 0.01
                ) {
                    style {
                        width(100.percent)
                    }
                    onInput {
                        val value = it.value!!.toFloat()
                        greenValue = value
                        map.set("backgroundColorG", value)
                    }
                }
            }

            // Blue component slider
            var blueValue by remember { mutableStateOf(map.getBackgroundColorB()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                Text("Blue")
                RangeInput(
                    blueValue,
                    min = 0.0,
                    max = 1.0,
                    step = 0.01
                ) {
                    style {
                        width(100.percent)
                    }
                    onInput {
                        val value = it.value!!.toFloat()
                        blueValue = value
                        map.set("backgroundColorB", value)
                    }
                }
            }

            // Alpha component slider
            var alphaValue by remember { mutableStateOf(map.getBackgroundColorA()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                Text("Alpha")
                RangeInput(
                    alphaValue,
                    min = 0.0,
                    max = 1.0,
                    step = 0.01
                ) {
                    style {
                        width(100.percent)
                    }
                    onInput {
                        val value = it.value!!.toFloat()
                        alphaValue = value
                        map.set("backgroundColorA", value)
                    }
                }
            }

            // Preview of the current color
            Div({
                style {
                    marginBottom(0.5.r)
                    padding(0.5.r)
                    property("background-color", "rgba(${(redValue * 255).toInt()}, ${(greenValue * 255).toInt()}, ${(blueValue * 255).toInt()}, $alphaValue)")
                    property("border-radius", "4px")
                    property("height", "32px")
                }
            }) {
                // Empty div for color preview
            }

            // Ambience Intensity control
            Div({
                style {
                    marginBottom(1.r)
                }
            }) {
                Text("Ambience Intensity")

                var ambienceIntensity by remember { mutableStateOf(map.getAmbienceIntensity()) }

                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    RangeInput(
                        ambienceIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            ambienceIntensity = value
                            map.set("ambienceIntensity", value)
                        }
                    }
                }
            }

            // Sun Intensity control
            Div({
                style {
                    marginBottom(1.r)
                }
            }) {
                Text("Sun Intensity")

                var sunIntensity by remember { mutableStateOf(map.getSunIntensity()) }

                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    RangeInput(
                        sunIntensity,
                        min = 0.0,
                        max = 10.0,
                        step = 0.01
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            sunIntensity = value
                            map.set("sunIntensity", value)
                        }
                    }
                }
            }

            // Fog Density control
            Div({
                style {
                    marginBottom(1.r)
                }
            }) {
                Text("Fog Density")

                var fogDensity by remember { mutableStateOf(map.getFogDensity()) }

                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    RangeInput(
                        fogDensity,
                        min = 0.0,
                        max = 0.1,
                        step = 0.001
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            fogDensity = value
                            map.set("fogDensity", value)
                        }
                    }
                }
            }

            // Time of Day control
            Div({
                style {
                    marginBottom(1.r)
                }
            }) {
                Text("Time of Day")

                var timeOfDay by remember { mutableStateOf(map.getTimeOfDay()) }

                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    RangeInput(
                        timeOfDay,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            timeOfDay = value
                            map.set("timeOfDay", value)
                        }
                    }
                }

                // Display time labels
                Div({
                    style {
                        property("display", "flex")
                        property("justify-content", "space-between")
                        property("font-size", "0.8rem")
                        property("color", "#888")
                    }
                }) {
                    Div { Text("Midnight") }
                    Div { Text("Noon") }
                    Div { Text("Midnight") }
                }
            }
        }
    }
}
