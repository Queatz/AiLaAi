package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.dom.Img
import qr
import r

@Composable
fun QrImg(url: String, styles: StyleScope.() -> Unit = {}) {
    val qrCode = remember {
        url.qr
    }
    Img(src = qrCode) {
        style {
            borderRadius(1.r)
            styles()
        }
    }
}
