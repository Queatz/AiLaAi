package app.widget.form

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun FormFieldCheckbox(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    label: String,
    isEnabled: Boolean
) {
    Label {
        CheckboxInput(checked) {
            onChange {
                onChecked(it.value)
            }

            if (!isEnabled) {
                disabled()
            }
        }
        Span({
            style {
                marginLeft(0.5.r)
            }
        }) {
            Text(label)
        }
    }
}
