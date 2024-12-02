package app.widget.form

import androidx.compose.runtime.Composable
import app.components.TextBox
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width

@Composable
fun FormFieldInput(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    isEnabled: Boolean
) {

    TextBox(
        value = value,
        onValue = onValue,
        placeholder = placeholder,
        disabled = !isEnabled,
        styles = {
            width(100.percent)
        }
    )
}
