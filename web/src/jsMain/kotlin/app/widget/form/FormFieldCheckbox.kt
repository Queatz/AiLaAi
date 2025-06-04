package app.widget.form

import androidx.compose.runtime.Composable
import components.LabeledCheckbox

@Composable
fun FormFieldCheckbox(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    label: String,
    isEnabled: Boolean
) {
    LabeledCheckbox(
        value = checked,
        onValue = onChecked,
        text = label,
        enabled = isEnabled,
    )
}
