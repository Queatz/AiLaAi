package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.widget.form.FormFieldCheckbox
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
fun WeatherSection(map: Map? = null) {
    PanelSection(
        title = "Weather",
        icon = "cloud",
        enabled = map != null,
        initiallyExpanded = true,
        closeOtherPanels = true
    ) {
        if (map == null) {
            Text("No map loaded")
            return@PanelSection
        }

        // Snow Effect controls
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            // Enable/Disable checkbox
            var snowEnabled by remember { mutableStateOf(map.isSnowEffectEnabled()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                FormFieldCheckbox(
                    checked = snowEnabled,
                    onChecked = { checked ->
                        snowEnabled = checked
                        map.setSnowEffectEnabled(checked)
                    },
                    label = "Enable Snow",
                    isEnabled = true
                )
            }

            // Intensity slider
            var snowIntensity by remember { mutableStateOf(map.getSnowEffectIntensity()) }

            if (snowEnabled) {
                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    Text("Intensity")
                    RangeInput(
                        snowIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            snowIntensity = value
                            map.setSnowEffectIntensity(value)
                        }
                    }
                }
            }
        }

        // Rain Effect controls
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            // Enable/Disable checkbox
            var rainEnabled by remember { mutableStateOf(map.isRainEffectEnabled()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                FormFieldCheckbox(
                    checked = rainEnabled,
                    onChecked = { checked ->
                        rainEnabled = checked
                        map.setRainEffectEnabled(checked)
                    },
                    label = "Enable Rain",
                    isEnabled = true
                )
            }

            // Intensity slider
            var rainIntensity by remember { mutableStateOf(map.getRainEffectIntensity()) }

            if (rainEnabled) {
                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    Text("Intensity")
                    RangeInput(
                        rainIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            rainIntensity = value
                            map.setRainEffectIntensity(value)
                        }
                    }
                }
            }
        }

        // Dust Effect controls
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            // Enable/Disable checkbox
            var dustEnabled by remember { mutableStateOf(map.isDustEffectEnabled()) }

            Div({
                style {
                    marginBottom(0.5.r)
                }
            }) {
                FormFieldCheckbox(
                    checked = dustEnabled,
                    onChecked = { checked ->
                        dustEnabled = checked
                        map.setDustEffectEnabled(checked)
                    },
                    label = "Enable Dust",
                    isEnabled = true
                )
            }

            // Intensity slider
            var dustIntensity by remember { mutableStateOf(map.getDustEffectIntensity()) }

            if (dustEnabled) {
                Div({
                    style {
                        marginBottom(0.5.r)
                    }
                }) {
                    Text("Intensity")
                    RangeInput(
                        dustIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style {
                            width(100.percent)
                        }
                        onInput {
                            val value = it.value!!.toFloat()
                            dustIntensity = value
                            map.setDustEffectIntensity(value)
                        }
                    }
                }
            }
        }
    }
}
