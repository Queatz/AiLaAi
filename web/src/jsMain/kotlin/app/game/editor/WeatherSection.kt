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
        Div({ style { marginBottom(1.r) } }) {
            val snowEnabled = map.isSnowEffectEnabled()
            FormFieldCheckbox(
                checked = snowEnabled,
                onChecked = { map.setSnowEffectEnabled(it) },
                label = "Enable Snow",
                isEnabled = true
            )
            if (snowEnabled) {
                val snowIntensity = map.getSnowEffectIntensity()
                Div({ style { marginBottom(0.5.r) } }) {
                    Text("Intensity")
                    RangeInput(
                        snowIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style { width(100.percent) }
                        onInput {
                            val value = it.value!!.toFloat()
                            map.setSnowEffectIntensity(value)
                        }
                    }
                }
            }
        }

        // Rain Effect controls
        Div({ style { marginBottom(1.r) } }) {
            val rainEnabled = map.isRainEffectEnabled()
            FormFieldCheckbox(
                checked = rainEnabled,
                onChecked = { map.setRainEffectEnabled(it) },
                label = "Enable Rain",
                isEnabled = true
            )
            if (rainEnabled) {
                val rainIntensity = map.getRainEffectIntensity()
                Div({ style { marginBottom(0.5.r) } }) {
                    Text("Intensity")
                    RangeInput(
                        rainIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style { width(100.percent) }
                        onInput {
                            val value = it.value!!.toFloat()
                            map.setRainEffectIntensity(value)
                        }
                    }
                }
            }
        }

        // Dust Effect controls
        Div({ style { marginBottom(1.r) } }) {
            val dustEnabled = map.isDustEffectEnabled()
            FormFieldCheckbox(
                checked = dustEnabled,
                onChecked = { map.setDustEffectEnabled(it) },
                label = "Enable Dust",
                isEnabled = true
            )
            if (dustEnabled) {
                val dustIntensity = map.getDustEffectIntensity()
                Div({ style { marginBottom(0.5.r) } }) {
                    Text("Intensity")
                    RangeInput(
                        dustIntensity,
                        min = 0.0,
                        max = 1.0,
                        step = 0.01
                    ) {
                        style { width(100.percent) }
                        onInput {
                            val value = it.value!!.toFloat()
                            map.setDustEffectIntensity(value)
                        }
                    }
                }
            }
        }
    }
}
