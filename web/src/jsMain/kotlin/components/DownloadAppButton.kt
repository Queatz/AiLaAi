package components
import androidx.compose.runtime.Composable
import appString
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun DownloadAppButton() {
    A("/ailaai.apk", {
        style {
            display(DisplayStyle.InlineBlock)
            padding(1.r, 2.r)
            fontWeight(700)
            fontSize(18.px)
            borderRadius(2.r)
            color(Color.white)
            textDecoration("none")
            textAlign("center")
            property("box-shadow", "2px 2px 8px rgba(0, 0, 0, .25)")
            backgroundColor(Styles.colors.primary)
            background("linear-gradient(rgb(49, 171, 213), rgb(0, 102, 137))")
        }
    }) {
        Span { Text(" ${appString { downloadApp }}") }
        Br()
        Span({
            style {
                opacity(.75f)
                fontSize(80.percent)
            }
        }) { Text(appString { appTagline }) }
    }
}
