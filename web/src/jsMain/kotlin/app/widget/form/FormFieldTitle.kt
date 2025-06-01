package app.widget.form

import Styles
import androidx.compose.runtime.Composable
import application
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun FormFieldTitle(text: String, required: Boolean = false) {
    if (text.isBlank()) {
        return
    }

    Div({
        style {
            marginTop(1.r)
            fontSize(18.px)
            fontWeight("bold")
        }
    }) {
        Text(text)
        if (required) {
            Span({
                style {
                    paddingLeft(.125f.r)
                    color(Styles.colors.red)
                }

                title(application.appString { this.required })
            }) {
                Text("*")
            }
        }
    }
}
