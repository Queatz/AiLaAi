package app.widget.space

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.web.events.SyntheticMouseEvent
import app.dialog.dialog
import app.widget.WidgetStyles
import components.IconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.css.width
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
    onDuration: (SyntheticMouseEvent) -> Unit,
    onDelete: (SyntheticMouseEvent) -> Unit
) {
    val scope = rememberCoroutineScope()

    Div(
        attrs = { classes(WidgetStyles.slideshowControls) }
    ) {
        if (isPaused) {
            // Create new slide always
            IconButton(
                name = "add",
                title = "New slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onCreate(event) }
            )
        }
        // Rename/Delete only when there is at least one slide
        if (totalSlides > 0 && isPaused) {
            IconButton(
                name = "edit",
                // todo: translate
                title = "Rename slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onRename(event) }
            )
            IconButton(
                name = "timer",
                // todo: translate
                title = "Edit duration",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onDuration(event) }
            )
            IconButton(
                name = "delete",
                // todo: translate
                title = "Delete slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event ->
                    scope.launch {
                        val result = dialog(
                            // todo: translate
                            title = "Delete slide",
                            // todo: translate
                            confirmButton = "Delete",
                            // todo: translate
                            cancelButton = "Cancel"
                        )

                        if (result == true) {
                            onDelete(event)
                        }
                    }
                }
            )
        }

        if (isPaused) {
            Div(
                attrs = {
                    style {
                        margin(.5.r)
                        backgroundColor(rgb(255, 255, 255))
                        width(1.px)
                        opacity(.25f)
                    }
                }
            )
        }

        // Navigation and play/pause only when more than one slide
        if (totalSlides > 1) {
            IconButton(
                name = "navigate_before",
                // todo: translate
                title = "Previous slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onPrevious(event) }
            )
        }
        // Slide counter
        Span(attrs = { classes(WidgetStyles.slideshowControlsCounter) }) {
            Text("${(currentSlide + 1).coerceAtMost(totalSlides)} / $totalSlides")
        }
        // Next button only when more than one slide
        if (totalSlides > 1) {
            IconButton(
                name = "navigate_next",
                // todo: translate
                title = "Next slide",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onNext(event) }
            )
        }

        Div(
            attrs = {
                style {
                    margin(8.px)
                    backgroundColor(rgb(255, 255, 255))
                    width(1.px)
                    opacity(.25f)
                }
            }
        )

        if (totalSlides > 1) {
            IconButton(
                name = if (isPaused) "play_arrow" else "pause",
                // todo: translate
                title = if (isPaused) "Resume slideshow" else "Pause slideshow",
                styles = { color(rgb(255, 255, 255)) },
                onClick = { event -> onPause(event) }
            )
        }

        // Exit slideshow
        IconButton(
            name = "close",
            // todo: translate
            title = "Exit slideshow",
            styles = {
                color(rgb(255, 255, 255))
            },
            onClick = { event -> onExit(event) }
        )
    }
}
