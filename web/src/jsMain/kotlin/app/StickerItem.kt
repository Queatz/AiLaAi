package app
import androidx.compose.runtime.*
import baseUrl
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun StickerItem(
    photo: String,
    message: String? = null,
    size: CSSNumeric = 64.px,
    title: String? = null,
    messageAlign: AlignItems = AlignItems.Start,
    onClick: () -> Unit
) {
    var showMessage by remember {
        mutableStateOf(false)
    }
    Div({
        style {
            width(size)
            height(size)
            borderRadius(1.r / 2)
            backgroundImage("url('$baseUrl$photo')")
            backgroundRepeat("no-repeat")
            backgroundSize("contain")
            backgroundPosition("center")
            marginRight(.5.r)
            cursor("pointer")
            flexShrink(0)
            position(Position.Relative)
        }
        onMouseEnter {
            showMessage = true
        }
        onMouseLeave {
            showMessage = false
        }
        if (title != null) {
            title(title)
        }
        onClick {
            onClick()
        }
    }) {
        if (showMessage) {
            message?.let { message ->
                Div({
                    classes(AppStyles.stickerMessage)
                    style {
                        position(Position.Absolute)
                        top(50.percent)
                        if (messageAlign == AlignItems.End) {
                            right(0.r)
                            property("transform", "translateX(calc(1rem + 100%)) translateY(-50%)")
                        } else {
                            left(0.r)
                            property("transform", "translateX(calc(-1rem - 100%)) translateY(-50%)")
                        }
                        property("box-shadow", "2px 2px 8px rgba(0, 0, 0, .25)")
                    }
                }) {
                    Text(message)
                }
            }
        }
    }
}
