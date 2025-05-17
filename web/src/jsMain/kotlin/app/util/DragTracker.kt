package app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent

/**
 * A utility to track mouse drag events even when the cursor leaves the component's bounds.
 *
 * @param enabled Whether drag tracking is enabled
 * @param onDragStart Callback when drag starts, provides the initial mouse position
 * @param onDrag Callback during drag, provides the current mouse position
 * @param onDragEnd Callback when drag ends, provides the final mouse position
 * @return A function to be called on mouseDown to start tracking
 */
@Composable
fun rememberDragTracker(
    enabled: Boolean = true,
    onDragStart: (clientX: Int, clientY: Int) -> Unit = { _, _ -> },
    onDrag: (clientX: Int, clientY: Int) -> Unit = { _, _ -> },
    onDragEnd: (clientX: Int, clientY: Int) -> Unit = { _, _ -> }
): (SyntheticMouseEvent) -> Unit {
    // Create mutable references to the listeners
    var mouseMoveListenerRef by remember { mutableStateOf<EventListener?>(null) }
    var mouseUpListenerRef by remember { mutableStateOf<EventListener?>(null) }

    // Clean up event listeners if the component is disposed while dragging
    DisposableEffect(enabled) {
        onDispose {
            mouseMoveListenerRef?.let { listener ->
                document.removeEventListener("mousemove", listener)
                mouseMoveListenerRef = null
            }
            mouseUpListenerRef?.let { listener ->
                document.removeEventListener("mouseup", listener)
                mouseUpListenerRef = null
            }
        }
    }

    // Return a function that starts the drag tracking
    return remember(enabled, onDragStart) {
        { event: SyntheticMouseEvent ->
            // Extract clientX and clientY from the event, regardless of its specific type
            val clientX = event.clientX
            val clientY = event.clientY

            if (enabled) {
                event.preventDefault()

                onDragStart(clientX, clientY)

                mouseMoveListenerRef =
                    EventListener { event ->
                        if (event is MouseEvent) {
                            onDrag(event.clientX, event.clientY)
                        }
                    }.also {
                        document.addEventListener("mousemove", it)
                    }

                mouseUpListenerRef =
                    EventListener { event ->
                        if (event is MouseEvent) {
                            onDragEnd(event.clientX, event.clientY)

                            // Clean up event listeners when drag ends
                            mouseMoveListenerRef?.let { listener ->
                                document.removeEventListener("mousemove", listener)
                                mouseMoveListenerRef = null
                            }
                            mouseUpListenerRef?.let { listener ->
                                document.removeEventListener("mouseup", listener)
                                mouseUpListenerRef = null
                            }
                        }
                    }.also {
                        document.addEventListener("mouseup", it)
                    }
            }
        }
    }
}

/**
 * Extension function to calculate position within an element based on mouse coordinates
 *
 * @param element The HTML element to calculate position within
 * @param clientX The mouse X coordinate
 * @param clientY The mouse Y coordinate
 * @return A Pair of normalized positions (0.0 to 1.0) within the element (x, y)
 */
fun calculateNormalizedPosition(element: HTMLElement, clientX: Int, clientY: Int): Pair<Double, Double> {
    val rect = element.getBoundingClientRect()
    val x = (clientX - rect.left).coerceIn(0.0, rect.width) / rect.width
    val y = (clientY - rect.top).coerceIn(0.0, rect.height) / rect.height
    return x to y
}
