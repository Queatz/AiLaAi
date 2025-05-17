package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LabeledSwitch(
    value: Boolean,
    onValue: (Boolean) -> Unit,
    onChange: (Boolean) -> Unit,
    title: String? = null,
    border: Boolean = false,
    styles: StyleScope.() -> Unit = {}
) {
    Div({
        style {
            // Apply any custom styles
            styles()
        }
    }) {
        // Use flexbox to place the switch and label side by side
        Div({
            style {
                property("display", "flex")
                property("align-items", "center")
            }
        }) {
            // Include the original Switch component
            Switch(
                value = value,
                onValue = onValue,
                onChange = onChange,
                // Still pass title for tooltip functionality
                title = title,
                border = border
            )
            
            // Add the visible text label if title is provided
            if (title != null) {
                Span({
                    style {
                        marginLeft(8.px)
                    }
                }) {
                    Text(title)
                }
            }
        }
    }
}
