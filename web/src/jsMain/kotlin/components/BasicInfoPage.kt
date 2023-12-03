package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun BasicInfoPage(title: String, about: String) {
    Div({
        style {
            width(100.percent)
        }
    }) {
        Div({
            style {
                marginBottom(.5.r)
                fontSize(32.px)
            }
        }) {
            Text(title)
        }
        Div({
            style {
                opacity(.5f)
            }
        }) {
            Text(about)
        }
    }
}
