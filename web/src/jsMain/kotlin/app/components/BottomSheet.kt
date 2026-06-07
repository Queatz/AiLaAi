package app.components

import Styles
import androidx.compose.runtime.*
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.math.abs

enum class BottomSheetState {
    Collapsed,
    Half,
    Full
}

@Composable
fun BottomSheet(
    state: BottomSheetState = BottomSheetState.Collapsed,
    onStateChange: (BottomSheetState) -> Unit = {},
    content: @Composable () -> Unit
) {
    var currentY by remember { mutableStateOf(0.0) }
    var isDragging by remember { mutableStateOf(false) }
    var startY by remember { mutableStateOf(0.0) }
    var startOffset by remember { mutableStateOf(0.0) }

    val sheetHeight = window.innerHeight * 0.95
    val fullY = 0.0
    val halfY = sheetHeight * 0.45
    val collapsedY = sheetHeight - 200.0 // Peak height

    val offsets = remember(sheetHeight) {
        mapOf(
            BottomSheetState.Full to fullY,
            BottomSheetState.Half to halfY,
            BottomSheetState.Collapsed to collapsedY
        )
    }

    LaunchedEffect(state, sheetHeight) {
        if (!isDragging) {
            currentY = offsets[state] ?: collapsedY
        }
    }

    Div({
        classes(Styles.bottomSheetContainer)
        style {
            property("transform", "translateY(${currentY}px)")
            if (isDragging) {
                property("transition", "none")
            }
        }
    }) {
        // Handle
        Div({
            classes(Styles.bottomSheetHandleContainer)
            ref { element ->
                val downListener = { event: Event ->
                    event as PointerEvent
                    if (event.button.toInt() == 0) {
                        isDragging = true
                        startY = event.clientY.toDouble()
                        startOffset = currentY
                        try {
                            element.setPointerCapture(event.pointerId)
                        } catch (e: Exception) {
                        }
                    }
                    Unit
                }
                val moveListener = { event: Event ->
                    event as PointerEvent
                    if (isDragging) {
                        val clientY = event.clientY.toDouble()
                        val deltaY = clientY - startY
                        currentY = (startOffset + deltaY).coerceAtLeast(fullY).coerceAtMost(collapsedY)
                    }
                    Unit
                }
                val stopDragging = { event: Event ->
                    event as PointerEvent
                    if (isDragging) {
                        isDragging = false
                        val closest = offsets.minByOrNull { abs(it.value - currentY) }?.key ?: BottomSheetState.Collapsed
                        onStateChange(closest)
                        currentY = offsets[closest] ?: collapsedY
                    }
                    Unit
                }
                element.addEventListener("pointerdown", downListener)
                element.addEventListener("pointermove", moveListener)
                element.addEventListener("pointerup", stopDragging)
                element.addEventListener("pointercancel", stopDragging)
                element.addEventListener("lostpointercapture", stopDragging)
                onDispose {
                    element.removeEventListener("pointerdown", downListener)
                    element.removeEventListener("pointermove", moveListener)
                    element.removeEventListener("pointerup", stopDragging)
                    element.removeEventListener("pointercancel", stopDragging)
                    element.removeEventListener("lostpointercapture", stopDragging)
                }
            }
        }) {
            Div({ classes(Styles.bottomSheetHandle) })
        }

        // Content
        Div({
            classes(Styles.bottomSheetContent)
        }) {
            content()
        }
    }
}
