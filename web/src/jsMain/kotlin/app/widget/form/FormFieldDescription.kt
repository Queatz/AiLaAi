package app.widget.form

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun FormFieldDescription(text: String) {
    if (text.isBlank()) {
        return
    }

    Div {
        Text(text)
    }
}
