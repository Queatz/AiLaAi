package components

import androidx.compose.runtime.Composable
import app.compose.rememberDarkMode
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.max
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.attributes.step
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun RangeSlider(
    minValue: Int,
    maxValue: Int,
    minLimit: Int,
    maxLimit: Int,
    onValueChange: (Int, Int) -> Unit,
    label: String? = null
) {
    val darkMode = rememberDarkMode()
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(0.5.cssRem)
            width(100.percent)
            padding(0.5.cssRem)
            minWidth(120.px)
        }
    }) {
        if (label != null) {
            Span({
                style {
                    fontSize(14.px)
                    opacity(0.8)
                    marginBottom(4.px)
                    color(if (darkMode) Color("white") else Color("black"))
                }
            }) {
                Text("$label • ")
                B {
                    Text("$minValue - $maxValue")
                }
            }
        }

        Div({
            style {
                position(Position.Relative)
                height(24.px)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
            }
        }) {
            // Track background
            Div({
                style {
                    position(Position.Absolute)
                    height(4.px)
                    width(100.percent)
                    backgroundColor(if (darkMode) Color("#444") else Color("#e0e0e0"))
                    borderRadius(2.px)
                }
            })

            // Active track (the blue part between knobs)
            Div({
                style {
                    position(Position.Absolute)
                    height(6.px)
                    val range = maxLimit - minLimit
                    val leftPercent = ((minValue - minLimit).toDouble() / range * 100).coerceIn(0.0, 100.0)
                    val rightPercent = ((maxValue - minLimit).toDouble() / range * 100).coerceIn(0.0, 100.0)
                    left(leftPercent.percent)
                    width((rightPercent - leftPercent).percent)
                    backgroundColor(Color("#007bff"))
                    borderRadius(3.px)
                    property("z-index", "1")
                }
            })

            // Hidden range inputs that act as knobs
            Input(InputType.Range) {
                style {
                    position(Position.Absolute)
                    width(100.percent)
                    height(0.px)
                    property("-webkit-appearance", "none")
                    property("appearance", "none")
                    backgroundColor(Color("transparent"))
                    property("z-index", "3")
                    outline("none")
                    property("touch-action", "none")
                    
                    // Style the thumb
                    property("&::-webkit-slider-thumb", "pointer-events: auto; -webkit-appearance: none; appearance: none; width: 24px; height: 24px; border-radius: 50%; background: #007bff; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.3);")
                    property("&::-moz-range-thumb", "pointer-events: auto; width: 24px; height: 24px; border-radius: 50%; background: #007bff; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.3);")
                }
                min(minLimit.toString())
                max(maxLimit.toString())
                step(1)
                value(minValue)
                onInput {
                    val newValue = it.value?.toInt() ?: minValue
                    onValueChange(minOf(newValue, maxValue), maxOf(newValue, maxValue))
                }
            }

            Input(InputType.Range) {
                style {
                    position(Position.Absolute)
                    width(100.percent)
                    height(0.px)
                    property("-webkit-appearance", "none")
                    property("appearance", "none")
                    backgroundColor(Color("transparent"))
                    property("z-index", "4")
                    outline("none")
                    property("touch-action", "none")

                    property("&::-webkit-slider-thumb", "pointer-events: auto; -webkit-appearance: none; appearance: none; width: 24px; height: 24px; border-radius: 50%; background: #007bff; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.3);")
                    property("&::-moz-range-thumb", "pointer-events: auto; width: 24px; height: 24px; border-radius: 50%; background: #007bff; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.3);")
                }
                min(minLimit.toString())
                max(maxLimit.toString())
                step(1)
                value(maxValue)
                onInput {
                    val newValue = it.value?.toInt() ?: maxValue
                    onValueChange(minOf(minValue, newValue), maxOf(minValue, newValue))
                }
            }
        }
    }
}
