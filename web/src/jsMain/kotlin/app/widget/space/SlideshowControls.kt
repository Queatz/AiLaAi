package app.widget.space

import androidx.compose.runtime.Composable
import androidx.compose.web.events.SyntheticMouseEvent
import app.widget.WidgetStyles
import components.IconButton
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

/**
 * Controls for navigating through slides during slideshow mode
 */
@Composable
fun SlideshowControls(
    currentSlide: Int,
    totalSlides: Int,
    isPaused: Boolean,
    onPrevious: (SyntheticMouseEvent) -> Unit,
    onPause: (SyntheticMouseEvent) -> Unit,
    onNext: (SyntheticMouseEvent) -> Unit,
    onExit: (SyntheticMouseEvent) -> Unit,
    onCreate: (SyntheticMouseEvent) -> Unit,
    onRename: (SyntheticMouseEvent) -> Unit,
    onDelete: (SyntheticMouseEvent) -> Unit
) {
    Div(
        attrs = { classes(WidgetStyles.slideshowControls) }
    ) {
        // Create new slide always
        IconButton(
            name = "add",
            title = "New Slide",
            styles = { color(rgb(255, 255, 255)) },
            onClick = { event -> onCreate(event) }
        )
        // Rename/Delete only when there is at least one slide
        if (totalSlides > 0) {
            IconButton(
                name = "edit",
                title = "Rename Slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onRename(event) }
            )
            IconButton(
                name = "delete",
                title = "Delete Slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onDelete(event) }
            )
        }
        // Navigation and play/pause only when more than one slide
        if (totalSlides > 1) {
            IconButton(
                name = "navigate_before",
                title = "Previous Slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onPrevious(event) }
            )
            IconButton(
                name = if (isPaused) "play_arrow" else "pause",
                title = if (isPaused) "Resume Slideshow" else "Pause Slideshow",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onPause(event) }
            )
        }
        // Slide counter
        Span(attrs = { classes(WidgetStyles.slideshowControlsCounter) }) {
            Text("${currentSlide + 1} / $totalSlides")
        }
        // Next button only when more than one slide
        if (totalSlides > 1) {
            IconButton(
                name = "navigate_next",
                title = "Next Slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onNext(event) }
            )
        }
        // Exit slideshow
        IconButton(
            name = "close",
            title = "Exit Slideshow",
            styles = {
                color(rgb(255, 255, 255))
                margin(0.r, 0.r, 0.r, 1.r)
            },
            onClick = { event -> onExit(event) }
        )
    }
}
