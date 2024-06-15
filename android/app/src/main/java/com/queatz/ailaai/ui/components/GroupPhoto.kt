package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.nullIfBlank
import com.queatz.ailaai.ui.theme.pad

private fun Modifier.multiPhoto(size: Dp, padding: Dp): Modifier = composed {
    this
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
}

private fun Modifier.singlePhoto(size: Dp, padding: Dp, border: Boolean): Modifier = composed {
    this
        .padding(padding)
        .requiredSize(size)
        .bordered(border)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.secondaryContainer)
}

@Composable
fun GroupPhoto(
    photos: List<ContactPhoto>,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    padding: Dp = 1.pad,
    border: Boolean = false
) {
    if (photos.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .singlePhoto(size, padding, border)
                .then(modifier)
        ) {}
    } else if (photos.size == 1) {
        val contact = photos.firstOrNull()
        val photo = contact?.photo?.nullIfBlank?.let { api.url(it) }
        if (photo == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .singlePhoto(size, padding, border)
                    .then(modifier)
            ) {
                Text(
                    contact?.name?.take(1) ?: "",
                    style = if (size >= 64.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            AsyncImage(
                model = photo,
                contentDescription = "",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .padding(padding)
                    .requiredSize(size)
                    .bordered(border)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .then(modifier)
            )
        }
    } else {
        val show = remember(photos) {
            photos.sortedByDescending { it.seen?.toEpochMilliseconds() ?: 0L }
        }
        Box(
            modifier = Modifier
                .padding(padding)
                .requiredSize(size)
                .then(modifier)
        ) {
            listOf(1 to Alignment.TopEnd, 0 to Alignment.BottomStart).forEach { (index, alignment) ->
                val photo = show[index].photo?.let { api.url(it) }
                if (photo == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(alignment)
                            .multiPhoto(size, padding)
                    ) {
                        Text(
                            show[index].name.take(1),
                            style = if (size >= 64.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    AsyncImage(
                        model = photo,
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .align(alignment)
                            .multiPhoto(size, padding)
                    )
                }
            }
        }
    }
}

private fun Modifier.bordered(border: Boolean) = composed {
    if (border) {
        this.border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
    } else {
        this
    }
}
