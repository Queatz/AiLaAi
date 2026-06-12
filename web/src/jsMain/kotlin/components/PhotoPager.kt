package components

import androidx.compose.runtime.*
import appString
import baseUrl
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Img
import org.w3c.dom.HTMLDivElement
import r

@Composable
fun PhotoPager(
    photos: List<String>,
    onPhotoClick: ((url: String) -> Unit)? = null,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: (@Composable ElementScope<HTMLDivElement>.() -> Unit)? = null,
) {
    var photoIndex by remember(photos) { mutableStateOf(0) }

    if (photos.isEmpty()) return

    val currentPhoto = photos[photoIndex]
    val url = "$baseUrl$currentPhoto"

    Div({
        if (attrs != null) {
            attrs()
        }
        style {
            backgroundImage("url($url)")
            backgroundPosition("center")
            backgroundSize("cover")
            position(Position.Relative)
        }

        if (onPhotoClick != null) {
            onClick {
                onPhotoClick(url)
            }
        }

        var startX = 0.0

        onTouchStart {
            startX = it.touches.item(0)?.clientX?.toDouble() ?: 0.0
        }

        onTouchEnd {
            val endX = it.changedTouches.item(0)?.clientX?.toDouble() ?: 0.0
            val diff = startX - endX
            if (kotlin.math.abs(diff) > 50) {
                if (diff > 0) {
                    photoIndex = (photoIndex + 1) % photos.size
                } else {
                    photoIndex = (photoIndex - 1 + photos.size) % photos.size
                }
            }
        }
    }) {
        content?.invoke(this)

        (1..2).forEach { i ->
            val preloadIndex = (photoIndex + i) % photos.size
            if (preloadIndex != photoIndex) {
                Img(src = "$baseUrl${photos[preloadIndex]}", attrs = {
                    style {
                        display(DisplayStyle.None)
                    }
                })
            }
        }

        if (photos.size > 1) {
            IconButton("chevron_left", appString { previous }, styles = {
                position(Position.Absolute)
                left(0.5.r)
                top(50.percent)
                property("transform", "translateY(-50%)")
                property("background-color", "rgba(0, 0, 0, 0.25)")
                borderRadius(50.percent)
                color(Color.white)
            }) {
                photoIndex = (photoIndex - 1 + photos.size) % photos.size
            }
            IconButton("chevron_right", appString { next }, styles = {
                position(Position.Absolute)
                right(0.5.r)
                top(50.percent)
                property("transform", "translateY(-50%)")
                property("background-color", "rgba(0, 0, 0, 0.25)")
                borderRadius(50.percent)
                color(Color.white)
            }) {
                photoIndex = (photoIndex + 1) % photos.size
            }

            Div({
                style {
                    position(Position.Absolute)
                    bottom(0.5.r)
                    left(50.percent)
                    property("transform", "translateX(-50%)")
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Row)
                    gap(0.25.r)
                    property("pointer-events", "none")
                }
            }) {
                photos.forEachIndexed { index, _ ->
                    Div({
                        style {
                            width(0.4.r)
                            height(0.4.r)
                            borderRadius(50.percent)
                            backgroundColor(if (index == photoIndex) Color.white else rgba(255, 255, 255, 0.5))
                            property("box-shadow", "0 1px 2px rgba(0, 0, 0, 0.25)")
                            property("transition", "background-color 0.2s")
                        }
                    })
                }
            }
        }
    }
}
