package com.queatz.ailaai.extensions

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
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
