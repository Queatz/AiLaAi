package com.queatz.ailaai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.queatz.ailaai.data.api

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoItem(photo: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    var aspect by remember(photo) {
        mutableFloatStateOf(0.75f)
    }
    var isLoaded by remember(photo) {
        mutableStateOf(false)
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(api.url(photo))
            .crossfade(true)
            .build(),
        alpha = if (isLoaded) 1f else .125f,
//        placeholder = rememberVectorPainter(Icons.Outlined.Photo),
        contentScale = ContentScale.Fit,
        onSuccess = {
            isLoaded = true
            aspect = it.result.image.width.toFloat() / it.result.image.height.toFloat()
        },
        contentDescription = "",
        alignment = Alignment.Center,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp /* Card elevation */))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .heightIn(min = 80.dp, max = 320.dp)
            .widthIn(min = 80.dp, max = 320.dp)
            .aspectRatio(aspect)
    )
}
