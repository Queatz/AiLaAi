package com.queatz.ailaai.extensions

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

suspend fun LazyGridState.scrollToTop() {
    if (firstVisibleItemIndex > 2) {
        scrollToItem(2)
    }

    animateScrollToItem(0)
}

suspend fun LazyListState.scrollToTop() {
    if (firstVisibleItemIndex > 2) {
        scrollToItem(2)
    }

    animateScrollToItem(0)
}

@Composable
fun Float.inDp(): Dp =
    (this / LocalContext.current.resources.displayMetrics.density).dp

@Composable
fun Int.inDp(): Dp =
    (this / LocalContext.current.resources.displayMetrics.density).dp

fun Uri.isVideo(context: Context): Boolean {
    return context.contentResolver.getType(this)?.startsWith("video/") == true
}

fun Uri.isPhoto(context: Context): Boolean {
    return context.contentResolver.getType(this)?.startsWith("image/") == true
}

@Composable
fun LazyGridState.rememberAutoplayIndex() = remember {
    derivedStateOf {
        firstVisibleItemIndex + (
                if ((firstVisibleItemScrollOffset > (layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height
                        ?: 0) / 2)
                ) 1 else 0
            )
    }
}

@Composable
fun LazyGridState.isAtTop() = remember {
    derivedStateOf {
        firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    }
}

fun Modifier.fadingEdge(viewport: Size, scrollState: ScrollState) = then(
    Modifier
        .graphicsLayer(alpha = 0.99f)
        .drawWithContent {
            drawContent()

            val h = scrollState.value.toFloat().coerceAtMost(viewport.height / 3f)
            val h2 = (
                    scrollState.maxValue.toFloat() - scrollState.value.toFloat()
                    ).coerceAtMost(viewport.height / 3f)

            if (scrollState.value != 0) {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = h + scrollState.value,
                        endY = 0.0f + scrollState.value
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

            if (scrollState.value != scrollState.maxValue) {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = viewport.height + scrollState.value - h2,
                        endY = viewport.height + scrollState.value
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
)

fun Modifier.horizontalFadingEdge(viewport: Size, scrollState: ScrollState) = then(
    Modifier
        .graphicsLayer(alpha = 0.99f)
        .drawWithContent {
            drawContent()

            val w = scrollState.value.toFloat().coerceAtMost(viewport.width / 6f)
            val w2 = (
                    scrollState.maxValue.toFloat() - scrollState.value.toFloat()
                    ).coerceAtMost(viewport.width / 6f)

            if (scrollState.value != 0) {
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = w + scrollState.value,
                        endX = 0.0f + scrollState.value
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

            if (scrollState.value != scrollState.maxValue) {
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = viewport.width - w2 + scrollState.value,
                        endX = viewport.width + scrollState.value
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
)

fun Modifier.fadingEdge(viewport: Size, scrollState: LazyListState, factor: Float = 3f) = then(
    Modifier
        .graphicsLayer(alpha = 0.99f)
        .drawWithContent {
            drawContent()

            val fadeSize = viewport.height / factor
            val value = scrollState.firstVisibleItemScrollOffset
            if (scrollState.firstVisibleItemIndex != 0 || value != 0) {
                val h = when (scrollState.firstVisibleItemIndex == 0) {
                    true -> value.toFloat().coerceAtMost(fadeSize).coerceAtLeast(0f)
                    false -> fadeSize
                }

                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = h,
                        endY = 0.0f
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastVisibleItemInfo ->
                val scrollFromEnd = (lastVisibleItemInfo.offset + lastVisibleItemInfo.size) - viewport.height
                val isLastItemVisible = lastVisibleItemInfo.index == scrollState.layoutInfo.totalItemsCount - 1
                val h = when (isLastItemVisible) {
                    true -> scrollFromEnd.coerceAtMost(fadeSize).coerceAtLeast(0f)
                    false -> fadeSize
                }

                if (h <= 0) return@let

                if (!isLastItemVisible || scrollFromEnd != 0f) {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = viewport.height - h,
                            endY = viewport.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
        }
)

fun Modifier.horizontalFadingEdge(viewport: Size, scrollState: LazyListState, factor: Float = 5f) = then(
    Modifier
        .graphicsLayer(alpha = 0.99f)
        .drawWithContent {
            drawContent()

            val fadeSize = viewport.width / factor
            val value = scrollState.firstVisibleItemScrollOffset
            if (scrollState.firstVisibleItemIndex != 0 || value != 0) {
                val w = when (scrollState.firstVisibleItemIndex == 0) {
                    true -> value.toFloat().coerceAtMost(fadeSize).coerceAtLeast(0f)
                    false -> fadeSize
                }

                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = w,
                        endX = 0.0f
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastVisibleItemInfo ->
                val scrollFromEnd = (lastVisibleItemInfo.offset + lastVisibleItemInfo.size) - viewport.width
                val isLastItemVisible = lastVisibleItemInfo.index == scrollState.layoutInfo.totalItemsCount - 1
                val w = when (isLastItemVisible) {
                    true -> scrollFromEnd.coerceAtMost(fadeSize).coerceAtLeast(0f)
                    false -> fadeSize
                }

                if (w <= 0) return@let

                if (!isLastItemVisible || scrollFromEnd != 0f) {
                    drawRect(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startX = viewport.width - w,
                            endX = viewport.width
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
        }
)

fun Modifier.minAspectRatio(ratio: Float) = then(
    MaxAspectRatioModifier(ratio)
)

class MaxAspectRatioModifier(
    private val aspectRatio: Float,
) : LayoutModifier {
    init {
        require(aspectRatio > 0f) { "aspectRatio $aspectRatio must be > 0" }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(
            constraints.copy(
                maxHeight = (constraints.maxWidth.toFloat() / aspectRatio).toInt()
            )
        )
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(IntOffset.Zero)
        }
    }
}
