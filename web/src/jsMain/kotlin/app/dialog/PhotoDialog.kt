package app.dialog

import application
import baseUrl
import components.PhotoPager
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Img
import r

suspend fun photoDialog(src: String) = photoDialog(
    photos = listOf(src.removePrefix(baseUrl)),
    initialIndex = 0
)

suspend fun photoDialog(photos: List<String>, initialIndex: Int = 0) = dialog(
    title = null,
    confirmButton = application.appString { close },
    cancelButton = null
) { resolve ->
    if (photos.size == 1) {
        Img(src = "$baseUrl${photos[0]}") {
            style {
                borderRadius(1.r)
                maxHeight(75.vh)
                maxWidth(90.vw)
            }

            onClick {
                resolve(false)
            }
        }
    } else {
        PhotoPager(
            photos = photos,
            initialIndex = initialIndex,
            attrs = {
                style {
                    borderRadius(1.r)
                    width(80.vw)
                    height(75.vh)
                }

                onClick {
                    resolve(false)
                }
            }
        )
    }
}
