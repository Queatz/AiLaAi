package app.widget.space

import androidx.compose.runtime.Composable
import androidx.compose.web.events.SyntheticMouseEvent
import app.widget.WidgetStyles
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceItem
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * Panel that displays a list of slides and allows users to select a specific slide
 */
@Composable
fun SlideListPanel(
    slides: List<SpaceItem>,
    currentSlideIndex: Int,
    onSelectSlide: (Int, SyntheticMouseEvent) -> Unit
) {
    Div(attrs = { classes(WidgetStyles.slideListPanel) }) {
        // Header
        Div(attrs = { classes(WidgetStyles.slideListPanelHeader) }) {
            Text("Slides")
        }

        // Slide list
        if (slides.isEmpty()) {
            Div(attrs = { classes(WidgetStyles.slideListPanelEmpty) }) {
                Text("No slides available")
            }
        } else {
            slides.forEachIndexed { index, slide ->
                val slideContent = slide.content as? SpaceContent.Slide

                Div(
                    attrs = {
                        classes(WidgetStyles.slideListPanelItem)

                        if (index == currentSlideIndex) {
                            classes(WidgetStyles.slideListPanelItemSelected)
                        }

                        onClick { event -> onSelectSlide(index, event) }
                    }
                ) {
                    Text(slideContent?.title ?: "Slide ${index + 1}")
                }
            }
        }
    }
}
