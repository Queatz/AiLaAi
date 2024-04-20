package app.messaages

import androidx.compose.runtime.Composable
import app.AppStyles
import com.queatz.db.UrlAttachment
import kotlinx.browser.window
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun UrlPreview(url: UrlAttachment) {
    Div(
        {
            classes(AppStyles.urlPreview)

            // todo: translate
            title("Open link\n${url.url!!}")

            onClick {
                window.open(url.url!!, "_blank")
            }
        }
    ) {
        url.image?.let { image ->
            Img(image) {
                style {
                    width(100.percent)
                    alignSelf(AlignSelf.Center)
                }
            }
        }
        Div({
            style {
                padding(1.r)
            }
        }) {
            url.title?.let { title ->
                Div({
                    style {
                        fontWeight("bold")
                        fontSize(18.px)
                    }
                }) {
                    Text(title)
                }
            }
            url.description?.let { description ->
                Div({
                    style {
                        if (url.image != null || url.title != null) {
                            marginTop(.25.r)
                        }
                        fontSize(14.px)
                        opacity(.5)
                    }
                }) {
                    Text(description)
                }
            }
        }
    }
}
