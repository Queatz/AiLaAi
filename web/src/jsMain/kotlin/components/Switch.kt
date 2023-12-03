package components

import androidx.compose.runtime.Composable
import focusable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span

@Composable
fun Switch(
    value: Boolean,
    onValue: (Boolean) -> Unit,
    onChange: (Boolean) -> Unit,
    title: String? = null,
    styles: StyleScope.() -> Unit = {}
) {
    Label(attrs = {
        classes(Styles.switch)
        focusable()
        style {
            styles()
        }
        if (title != null) {
            title(title)
        }
    }) {
        CheckboxInput(value) {
            onChange {
                onValue(it.value)
            }
            onInput {
                onChange(it.value)
            }
        }
        Span({
            classes(Styles.switchSlider)
        })
    }
}
