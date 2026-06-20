package components

import androidx.compose.runtime.Composable
import baseUrl
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.gridColumn
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import r
import stories.StoryStyles

@Composable
fun PhotoTiledGrid(
    photos: List<String>,
    aspect: Float? = null,
    onPhotoClick: (index: Int) -> Unit,
) {
    if (photos.isEmpty()) return

    // Layout rules:
    //   1 photo  → full-width, aspect ratio applied if provided, else square
    //   2 photos → 2 equal columns, aspect ratio applied if provided, else square
    //   3 photos → 2 equal columns; last photo spans full width; always square
    //   4+ photos → 2 equal columns; show up to 4, last visible tile spans
    //               full width when the displayed count is odd; "+N" overlay on last tile; always square

    // A single photo with no explicit aspect ratio renders at its natural aspect
    if (photos.size == 1 && aspect == null) {
        NaturalPhotoTile(
            photo = photos.first(),
            onClick = { onPhotoClick(0) }
        )
        return
    }

    val displayPhotos = if (photos.size > 4) photos.take(4) else photos
    val remaining = photos.size - displayPhotos.size
    val isLastFullWidth = displayPhotos.size % 2 != 0 // odd count → last tile is full-width
    val useAspect = photos.size <= 2

    Div({
        style {
            display(DisplayStyle.Grid)
            property("grid-template-columns", "1fr 1fr")
            gap(0.5.r)
            width(100.percent)
            boxSizing("border-box")
        }
    }) {
        displayPhotos.forEachIndexed { index, photo ->
            val isLast = index == displayPhotos.lastIndex
            val showOverlay = isLast && remaining > 0
            val spanFull = isLast && isLastFullWidth
            val tileAspect = if (useAspect) aspect else null

            if (showOverlay) {
                OverlayPhotoTile(
                    photo = photo,
                    remaining = remaining,
                    spanFull = spanFull,
                    aspect = tileAspect,
                    onClick = { onPhotoClick(index) }
                )
            } else {
                SquarePhotoTile(
                    photo = photo,
                    spanFull = spanFull,
                    aspect = tileAspect,
                    onClick = { onPhotoClick(index) }
                )
            }
        }
    }
}

@Composable
private fun NaturalPhotoTile(
    photo: String,
    onClick: () -> Unit,
) {
    val url = "$baseUrl$photo"
    Img(src = url, attrs = {
        classes(StoryStyles.contentPhotosPhotoNoAspect)
        onClick { onClick() }
    })
}

@Composable
private fun SquarePhotoTile(
    photo: String,
    spanFull: Boolean,
    aspect: Float? = null,
    onClick: () -> Unit,
) {
    val url = "$baseUrl$photo"
    Div({
        classes(StoryStyles.contentPhotosPhoto)
        style {
            backgroundImage("url($url)")
            property("aspect-ratio", if (aspect != null) "$aspect" else "1 / 1")
            property("min-width", "unset")
            property("flex", "unset")
            property("max-height", "unset")
            width(100.percent)
            if (spanFull) {
                gridColumn("1 / -1")
            }
        }
        onClick { onClick() }
    })
}

@Composable
private fun OverlayPhotoTile(
    photo: String,
    remaining: Int,
    spanFull: Boolean,
    aspect: Float? = null,
    onClick: () -> Unit,
) {
    val url = "$baseUrl$photo"
    Div({
        style {
            backgroundImage("url($url)")
            backgroundPosition("center")
            backgroundSize("cover")
            borderRadius(1.r)
            cursor("pointer")
            display(DisplayStyle.Flex)
            justifyContent(JustifyContent.Center)
            property("align-items", "center")
            property("aspect-ratio", if (aspect != null) "$aspect" else "1 / 1")
            property("position", "relative")
            property("overflow", "hidden")
            width(100.percent)
            if (spanFull) {
                gridColumn("1 / -1")
            }
        }
        onClick { onClick() }
    }) {
        Div({
            style {
                property("position", "absolute")
                property("inset", "0")
                property("background-color", "rgba(0,0,0,0.45)")
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.Center)
                property("align-items", "center")
            }
        }) {
            Div({
                style {
                    color(Color.white)
                    fontSize(24.px)
                    fontWeight("bold")
                }
            }) {
                Text("+${remaining + 1}")
            }
        }
    }
}
