package com.queatz.ailaai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun LoadMore(
    hasMore: Boolean,
    permanent: Boolean = false,
    visible: Boolean = true,
    contentPadding: Dp = 0.dp,
    onLoadMore: () -> Unit
) {
    var isLoadingMore by rememberStateOf(true)
    var viewport by remember {
        mutableStateOf(Rect.Zero)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .onPlaced {
                viewport = it.boundsInParent()
            }
    ) {
        val loadMore = viewport.bottom > contentPadding.px
        LaunchedEffect(loadMore) {
            if(loadMore) {
                while (true) {
                    isLoadingMore = true
                    try {
                        onLoadMore()
                    } finally {
                        isLoadingMore = false
                        delay(1.seconds)
                    }
                }
            }
        }
        AnimatedVisibility(hasMore && isLoadingMore || permanent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.pad)
            ) {
                if (visible) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}
