package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun GroupPhoto(photos: List<String>, size: Dp = 64.dp) {
    val padding = PaddingDefault
    if (photos.size == 1) {
        AsyncImage(
            model = photos.firstOrNull()?.let { api.url(it) } ?: "",
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )
    } else if (photos.size >= 2) {
        val show = remember { photos.shuffled() }
        Box(
            modifier = Modifier
                .padding(padding)
                .requiredSize(size)
        ) {
            listOf(Alignment.TopEnd, Alignment.BottomStart).forEachIndexed { index, alignment ->
                AsyncImage(
                    model = show[index].let { api.url(it) },
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .align(alignment)
                        .let {
                            if (size < 64.dp) {
                                it.requiredSize(size / 1.5f)
                            } else {
                                it.padding(padding)
                                    .requiredSize(size / 2)
                            }
                        }
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                )
            }
        }
    }
}
