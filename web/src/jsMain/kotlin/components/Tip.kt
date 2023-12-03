package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun Tip(
    text: String,
    action: String,
    styles: StyleScope.() -> Unit = {},
    onAction: () -> Unit
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            border(1.px, LineStyle.Solid, Styles.colors.primary)
            padding(2.r)
            borderRadius(1.r)
            overflow("hidden")
            styles()
        }
    }) {
        Div({
            style {
                marginBottom(2.r)
                textAlign("center")
                property("word-break", "break-word")
                property("text-wrap", "balance")
            }
        }) {
            Text(text)
        }
        Button({
            classes(Styles.button)

            onClick {
                onAction()
            }

        }) {
            Text(action)
        }
    }
}
