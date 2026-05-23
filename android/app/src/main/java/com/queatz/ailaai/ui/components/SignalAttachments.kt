package com.queatz.ailaai.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.queatz.ailaai.data.Api
import com.queatz.ailaai.ui.theme.pad

@Composable
fun SignalAttachments(
    photo: String?,
    audio: String?,
    api: Api,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    if (photo.isNullOrBlank() && audio.isNullOrBlank()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        if (!photo.isNullOrBlank()) {
            AsyncImage(
                model = api.url(photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        }

        if (!audio.isNullOrBlank()) {
            Box(modifier = Modifier.weight(1f, fill = false).widthIn(max = 240.dp)) {
                Audio(api.url(audio))
            }
        }
    }
}

@Composable
fun SignalAttachmentsEditor(
    photo: Uri?,
    audio: String?,
    onClearPhoto: () -> Unit,
    onClearAudio: () -> Unit,
    api: Api,
    modifier: Modifier = Modifier
) {
    if (photo == null && audio == null) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        verticalAlignment = Alignment.Top,
        modifier = modifier
    ) {
        if (photo != null) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(photo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onClearPhoto,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Outlined.Close, null, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }

        if (audio != null) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 1.pad, vertical = 0.5f.pad)
            ) {
                Audio(if (audio.contains("://")) audio else api.url(audio), modifier = Modifier.weight(1f))
                IconButton(onClick = onClearAudio) {
                    Icon(Icons.Outlined.Close, null)
                }
            }
        }
    }
}
