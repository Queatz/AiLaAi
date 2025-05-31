package app.widget.form

import androidx.compose.runtime.Composable
import app.components.FlexInput
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width

@Composable
fun FormFieldInput(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    isEnabled: Boolean
) {
    FlexInput(
        value = value,
        onChange = onValue,
        placeholder = placeholder,
        enabled = isEnabled,
        styles = {
            width(100.percent)
        }
    )
}
