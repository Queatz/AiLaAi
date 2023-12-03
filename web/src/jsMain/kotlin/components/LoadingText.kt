package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun LoadingText(done: Boolean, text: String, content: @Composable () -> Unit) {
    if (done) {
        content()
    } else {
        Div({
            style {
                height(6.r)
                width(100.percent)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.Center)
                opacity(.5f)
            }
        }) {
            Text(text)
        }
    }
}
