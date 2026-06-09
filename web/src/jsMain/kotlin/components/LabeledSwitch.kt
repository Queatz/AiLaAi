package components

import androidx.compose.runtime.Composable
import app.compose.rememberDarkMode
import org.jetbrains.compose.web.css.*
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
    val darkMode = rememberDarkMode()
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
                        fontSize(14.px)
                        opacity(0.9)
                        color(if (darkMode) Color("white") else Color("black"))
                    }
                }) {
                    Text(title)
                }
            }
        }
    }
}
