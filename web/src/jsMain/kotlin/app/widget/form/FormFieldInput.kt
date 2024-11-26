package app.widget.form

import Styles
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.TextArea

@Composable
fun FormFieldInput(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    isEnabled: Boolean
) {
    TextArea(value) {
        classes(Styles.textarea)
        style {
            width(100.percent)
        }

        placeholder(placeholder)

        onInput {
            onValue(it.value)
        }

        if (!isEnabled) {
            disabled()
        }
    }
}
