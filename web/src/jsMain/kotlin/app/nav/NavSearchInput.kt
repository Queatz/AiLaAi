package app.nav

import androidx.compose.runtime.Composable
import app.components.FlexInput
import appString
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.StyleScope

@Composable
fun NavSearchInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = appString { search },
    autoFocus: Boolean = true,
    selectAll: Boolean = false,
    defaultMargins: Boolean = true,
    type: InputType<*>? = null,
    onDismissRequest: () -> Unit = {},
    styles: StyleScope.() -> Unit = {},
    onSubmit: (String) -> Unit = {}
) {
    FlexInput(
        value = value,
        onChange = onChange,
        placeholder = placeholder,
        singleLine = true,
        autoFocus = autoFocus,
        selectAll = selectAll,
        inputType = type,
        defaultMargins = defaultMargins,
        styles = {
            styles()
        },
        onDismissRequest = onDismissRequest,
        onSubmit = {
            onSubmit(value)
            true
        }
    )
}
