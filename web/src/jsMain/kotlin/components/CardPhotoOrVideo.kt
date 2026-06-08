package components

import Styles
import androidx.compose.runtime.*
import app.dialog.photoDialog
import appString
import baseUrl
import com.queatz.db.Card
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLVideoElement
import r

@Composable
fun CardPhotoOrVideo(
    card: Card,
    defaultWidth: Boolean = true,
    styles: StyleScope.() -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val allPhotos = remember(card.photo, card.photos) {
        listOfNotNull(card.photo) + (card.photos ?: emptyList())
    }
    var photoIndex by remember(allPhotos) { mutableStateOf(0) }

    if (allPhotos.isNotEmpty()) {
        val url = "$baseUrl${allPhotos[photoIndex]}"

        Div({
            style {
                if (defaultWidth) {
                    width(100.percent)
                }
                backgroundColor(Styles.colors.background)
                backgroundImage("url($url)")
                backgroundPosition("center")
                backgroundSize("cover")
                maxHeight(50.vh)
                property("aspect-ratio", "2")
                cursor("pointer")
                position(Position.Relative)
                styles()
            }

            onClick {
                scope.launch {
                    photoDialog(url)
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
                        photoIndex = (photoIndex + 1) % allPhotos.size
                    } else {
                        photoIndex = (photoIndex - 1 + allPhotos.size) % allPhotos.size
                    }
                }
            }
        }) {
            if (allPhotos.size > 1) {
                IconButton("chevron_left", appString { previous }, styles = {
                    position(Position.Absolute)
                    left(0.5.r)
                    top(50.percent)
                    property("transform", "translateY(-50%)")
                    property("background-color", "rgba(0, 0, 0, 0.25)")
                    borderRadius(50.percent)
                    color(Color.white)
                }) {
                    photoIndex = (photoIndex - 1 + allPhotos.size) % allPhotos.size
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
                    photoIndex = (photoIndex + 1) % allPhotos.size
                }
            }
        }
    } else card.video?.let {
        var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
//                            LaunchedEffect(videoElement) {
//                                if (videoElement != null) {
//                                    delay(250)
//                                    try {
////                                        if (window.navigator.getAutoplayPolicy)
//                                        videoElement!!.muted = false
//                                    } catch (e: Throwable) {
//                                        // ignore
//                                    }
//                                }
//                            }
        Video({
            attr("muted", "muted")
            attr("autoplay", "")
            attr("loop", "")
            attr("playsinline", "")
            style {
                property("object-fit", "cover")
                width(100.percent)
                backgroundColor(Styles.colors.background)
                property("aspect-ratio", "2")
                styles()
            }
            onClick {
                (it.target as? HTMLVideoElement)?.apply {
                    play()
                    muted = false
                }
            }
            // Do this so that auto-play works on page load, but unmute on page navigation
            ref { videoEl ->
                videoEl.onloadedmetadata = {
                    videoEl.muted = true
                    videoElement = videoEl
                    it
                }
                onDispose {  }
            }
        }) {
            Source({
                attr("src", "$baseUrl$it")
                attr("type", "video/webm")
            })
        }
    }
}
