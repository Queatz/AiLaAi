package com.queatz.ailaai.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
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
