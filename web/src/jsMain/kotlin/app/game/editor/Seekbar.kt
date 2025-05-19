package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.util.rememberDragTracker
import format1Decimal
import format3Decimals
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.times
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement


/**
 * Data class representing a marker on the seekbar
 * @param position Position of the marker in seconds
 * @param name Name of the marker
 * @param duration Duration of the marker in seconds (0 = play until end)
 */
data class SeekbarMarker(
    val position: Double,
    val name: String,
    val duration: Double = 0.0,
    val visible: Boolean = true
)

/**
 * Data class representing a keyframe on the seekbar
 * @param position Position of the keyframe in seconds
 * @param duration Optional duration of the keyframe in seconds
 */
data class SeekbarKeyframe(
    val position: Double,
    val duration: Double = 0.0
)

/**
 * A reusable Seekbar component used to scrub through an animation
 *
 * @param currentPosition Current position in the animation in seconds
 * @param markers List of markers to display on the seekbar
 * @param keyframes List of keyframes to display on the seekbar
 * @param onPositionChange Callback when the position changes (in seconds)
 */
@Composable
fun Seekbar(
    currentPosition: Double,
    markers: List<SeekbarMarker> = emptyList(),
    keyframes: List<SeekbarKeyframe> = emptyList(),
    onPositionChange: (Double) -> Unit
) {
    Style(SeekbarStyles)
    var isDragging by remember { mutableStateOf(false) }

    // Calculate total duration based on the last keyframe + its duration
    // Use keyframes.size and markers.size to force recomposition when keyframes or markers are added/removed
    var totalDuration = remember(keyframes.size, keyframes.toString(), markers.size, markers.toString()) {
        if (keyframes.isEmpty()) {
            60.0 // Default duration if no keyframes
        } else {
            val lastKeyframe = keyframes.maxByOrNull { it.position + it.duration }!!
            lastKeyframe.position + lastKeyframe.duration
        }
    }

    // Normalize position to 0.0-1.0 range
    val normalizePosition = remember(totalDuration, keyframes.size, keyframes.toString(), markers.size, markers.toString()) {
        { seconds: Double ->
            (seconds / totalDuration).coerceIn(0.0, 1.0)
        }
    }

    // Convert normalized position (0.0-1.0) back to seconds
    val denormalizePosition = remember(totalDuration, keyframes.size, keyframes.toString(), markers.size, markers.toString()) {
        { normalized: Double ->
            (normalized * totalDuration).coerceIn(0.0, totalDuration)
        }
    }

    Div({
        classes(SeekbarStyles.seekbarContainer)
    }) {
        // Markers container (above the bar)
        Div({
            classes(SeekbarStyles.seekbarMarkersContainer)
        }) {
            // Render marker labels (only visible ones)
            markers.filter { it.visible }.forEach { marker ->
                Div({
                    classes(SeekbarStyles.seekbarMarkerLabel)
                    style {
                        left((normalizePosition(marker.position) * 100).percent)
                    }
                    // Add click handler to marker name
                    onClick {
                        onPositionChange(marker.position)
                        it.stopPropagation()
                    }
                    title("Click to set position to ${marker.position.format3Decimals()} seconds")
                    style {
                        cursor("pointer")
                    }
                }) {
                    Text(marker.name)
                }
            }
        }

        // Store a reference to the seekbar element
        var seekbarElement by remember { mutableStateOf<HTMLElement?>(null) }

        // Function to calculate position from mouse coordinates
        val calculatePosition = remember(totalDuration, keyframes.toString()) {
            { clientX: Int ->
                // Use the seekbarElement if available
                val position = seekbarElement?.let { element ->
                    val rect = element.getBoundingClientRect()
                    val x = clientX - rect.left
                    val normalizedPosition = (x / rect.width).coerceIn(0.0, 1.0)
                    denormalizePosition(normalizedPosition)
                }

                // If seekbarElement is not available or calculation failed, use a fallback
                position ?: currentPosition
            }
        }

        // Use the drag tracker to handle mouse events  
        val startDrag = rememberDragTracker(
            enabled = true,
            onDragStart = { clientX, _ ->
                isDragging = true
                // Calculate position directly when drag starts
                onPositionChange(calculatePosition(clientX))
            },
            onDrag = { clientX, _ ->
                if (isDragging) {
                    onPositionChange(calculatePosition(clientX))
                }
            },
            onDragEnd = { clientX, _ ->
                isDragging = false
                onPositionChange(calculatePosition(clientX))
            }
        )

        // Main seekbar bar
        Div({
            classes(SeekbarStyles.seekbarBar)


            onClick { event ->
                val element = event.currentTarget as? HTMLElement
                if (element != null) {
                    // Store the seekbar element
                    seekbarElement = element
                    val rect = element.getBoundingClientRect()
                    val x = event.clientX - rect.left
                    val normalizedPosition = (x / rect.width).coerceIn(0.0, 1.0)
                    onPositionChange(denormalizePosition(normalizedPosition))
                }
            }

            onMouseDown { event ->
                // Ensure the seekbar element is set
                val element = event.currentTarget as? HTMLElement
                if (element != null) {
                    seekbarElement = element
                    // Calculate position directly from the event coordinates
                    val rect = element.getBoundingClientRect()
                    val x = event.clientX - rect.left
                    val normalizedPosition = (x / rect.width).coerceIn(0.0, 1.0)
                    onPositionChange(denormalizePosition(normalizedPosition))
                    // Set isDragging to true
                    isDragging = true
                    // Then start the drag operation
                    startDrag(event)
                }
            }
        }) {
            // Render keyframes
            keyframes.forEach { keyframe ->
                // Keyframe circle
                Div({
                    classes(SeekbarStyles.seekbarKeyframe)
                    style {
                        left((normalizePosition(keyframe.position) * 100).percent)
                    }
                })

                // Keyframe duration line (if duration > 0)
                if (keyframe.duration > 0) {
                    Div({
                        classes(SeekbarStyles.seekbarKeyframeDuration)
                        style {
                            left((normalizePosition(keyframe.position) * 100).percent)
                            width((normalizePosition(keyframe.position + keyframe.duration) - normalizePosition(keyframe.position)) * 100.percent)
                        }
                    })
                }
            }

            // Render markers (only visible ones)
            markers.filter { it.visible }.forEach { marker ->
                Div({
                    classes(SeekbarStyles.seekbarMarker)
                    style {
                        left((normalizePosition(marker.position) * 100).percent)
                    }
                })

                // Render marker duration if it's set
                if (marker.duration > 0) {
                    Div({
                        classes(SeekbarStyles.seekbarKeyframeDuration)
                        style {
                            left((normalizePosition(marker.position) * 100).percent)
                            width((normalizePosition(marker.position + marker.duration) - normalizePosition(marker.position)) * 100.percent)
                            property("background-color", "rgba(0, 128, 255, 0.3)") // Blue color for music duration
                        }
                    })
                }
            }

            // Current frame indicator
            Div({
                classes(SeekbarStyles.seekbarCurrentFrame)
                style {
                    left((normalizePosition(currentPosition) * 100).percent)
                }
            })
        }
    }
}
