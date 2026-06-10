package components

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import app.compose.rememberDarkMode
import org.w3c.dom.HTMLElement
import org.w3c.dom.Touch
import org.w3c.dom.events.*
import Styles

@Composable
fun RangeSlider(
    minValue: Int,
    maxValue: Int,
    minLimit: Int,
    maxLimit: Int,
    onValueChange: (Int, Int) -> Unit,
    label: String? = null,
    steps: List<Int>? = null
) {
    val darkMode = rememberDarkMode()
    var dragging by remember { mutableStateOf<Handle?>(null) }
    
    // We need a ref to the slider container to calculate positions
    val containerRef = remember { mutableStateOf<HTMLElement?>(null) }

    fun getPercentFromPointer(clientX: Double): Double {
        val rect = containerRef.value?.getBoundingClientRect() ?: return 0.0
        val x = clientX - rect.left
        return (x / rect.width * 100).coerceIn(0.0, 100.0)
    }

    fun getValueFromPercent(percent: Double): Int {
        if (steps != null && steps.isNotEmpty()) {
            val index = (percent / 100 * (steps.size - 1)).toInt().coerceIn(0, steps.size - 1)
            return steps[index]
        }
        val range = maxLimit - minLimit
        return (minLimit + (percent / 100 * range)).toInt()
    }

    fun getPercentFromValue(value: Int): Double {
        if (steps != null && steps.isNotEmpty()) {
            val index = steps.indexOf(value).takeIf { it != -1 } ?: steps.mapIndexed { i, v -> i to v }
                .minByOrNull { kotlin.math.abs(it.second - value) }?.first ?: 0
            return (index.toDouble() / (steps.size - 1) * 100).coerceIn(0.0, 100.0)
        }
        val range = maxLimit - minLimit
        if (range == 0) return 0.0
        return ((value - minLimit).toDouble() / range * 100).coerceIn(0.0, 100.0)
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(0.5.cssRem)
            minWidth(120.px)
            flexGrow(1)
            padding(0.5.cssRem)
            position(Position.Relative)
        }
        // Handle global drag
        onMouseMove { event ->
            dragging?.let { handle ->
                val percent = getPercentFromPointer(event.clientX.toDouble())
                val value = getValueFromPercent(percent)
                
                when (handle) {
                    Handle.MIN -> {
                        if (value >= maxValue) {
                            onValueChange(value, value)
                        } else {
                            onValueChange(value, maxValue)
                        }
                    }
                    Handle.MAX -> {
                        if (value <= minValue) {
                            onValueChange(value, value)
                        } else {
                            onValueChange(minValue, value)
                        }
                    }
                }
            }
        }
        onMouseUp { dragging = null }
        onMouseLeave { dragging = null }
        onTouchMove { event ->
            dragging?.let { handle ->
                val touch = event.touches.item(0)
                touch?.let {
                    val percent = getPercentFromPointer(it.clientX.toDouble())
                    val value = getValueFromPercent(percent)
                    
                    when (handle) {
                        Handle.MIN -> {
                            if (value >= maxValue) {
                                onValueChange(value, value)
                            } else {
                                onValueChange(value, maxValue)
                            }
                        }
                        Handle.MAX -> {
                            if (value <= minValue) {
                                onValueChange(value, value)
                            } else {
                                onValueChange(minValue, value)
                            }
                        }
                    }
                }
            }
        }
        onTouchEnd { dragging = null }
    }) {
        if (label != null) {
            Span({
                style {
                    fontSize(14.px)
                    opacity(0.8)
                    color(if (darkMode) Color("white") else Color("black"))
                    whiteSpace("nowrap")
                    overflow("hidden")
                    property("text-overflow", "ellipsis")
                    position(Position.Absolute)
                    top(0.px)
                    left(0.5.cssRem)
                    right(0.5.cssRem)
                }
            }) {
                Text("$label ")
                B {
                    Text("$minValue - $maxValue")
                }
            }
        }

        Div({
            ref {
                containerRef.value = it
                onDispose { containerRef.value = null }
            }
            style {
                position(Position.Relative)
                height(24.px)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                // Thick track background
                property("touch-action", "none")
                marginTop(24.px)
            }
        }) {
            // Track background
            Div({
                style {
                    position(Position.Absolute)
                    height(6.px)
                    width(100.percent)
                    backgroundColor(if (darkMode) Styles.colors.dark.surface else Styles.colors.lightgray)
                    borderRadius(6.px)
                }
            })

            // Active track
            val leftPercent = getPercentFromValue(minValue)
            val rightPercent = getPercentFromValue(maxValue)
            val widthPercent = rightPercent - leftPercent
            val heightValue = 18.0 - (widthPercent / 100.0 * 12.0)

            Div({
                style {
                    position(Position.Absolute)
                    height(heightValue.px)
                    left(leftPercent.percent)
                    width(widthPercent.percent)
                    backgroundColor(Styles.colors.primary)
                    borderRadius(heightValue.px)
                }
            })

            // Min handle
            Div({
                style {
                    position(Position.Absolute)
                    height(24.px)
                    width(24.px)
                    borderRadius(50.percent)
                    backgroundColor(if (darkMode) Styles.colors.white else Styles.colors.primary)
                    left(leftPercent.percent)
                    marginLeft((-12).px)
                    cursor("pointer")
                    property("z-index", "2")
                }
                onMouseDown { it.preventDefault(); dragging = Handle.MIN }
                onTouchStart { it.preventDefault(); dragging = Handle.MIN }
            })

            // Max handle
            Div({
                style {
                    position(Position.Absolute)
                    height(24.px)
                    width(24.px)
                    borderRadius(50.percent)
                    backgroundColor(if (darkMode) Styles.colors.white else Styles.colors.primary)
                    left(rightPercent.percent)
                    marginLeft((-12).px)
                    cursor("pointer")
                    property("z-index", "2")
                }
                onMouseDown { it.preventDefault(); dragging = Handle.MAX }
                onTouchStart { it.preventDefault(); dragging = Handle.MAX }
            })
        }
    }
}

private enum class Handle {
    MIN, MAX
}
