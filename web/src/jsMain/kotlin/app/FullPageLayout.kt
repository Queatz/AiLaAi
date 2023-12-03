package app

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

@Composable
fun FullPageLayout(maxWidth: CSSpxValue? = 800.px, content: @Composable () -> Unit) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            width(100.percent)
            height(100.percent)
            overflowX("hidden")
            overflowY("auto")
        }
    }) {
        if (maxWidth == null) {
            content()
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    width(100.percent)
                    height(100.percent)
                    alignItems(AlignItems.Stretch)
                    maxWidth(maxWidth)
                    alignSelf(AlignSelf.Center)
                }
            }) {
                content()
            }
        }
    }
}
