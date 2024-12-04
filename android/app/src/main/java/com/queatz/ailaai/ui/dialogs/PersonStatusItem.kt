package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.components.LinkifyText
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.PersonStatus

@Composable
fun PersonStatusItem(
    status: PersonStatus,
    modifier: Modifier = Modifier,
    onShowPhoto: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        status.statusInfo?.let { info ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(.5f.pad, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(.25f.pad)
                        .size(12.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            color = info.color?.toColorInt()
                                ?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.background
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = .5f),
                                    Color.White.copy(alpha = 0f)
                                ),
                                center = Offset(
                                    4.5f.dp.px.toFloat(),
                                    4.5f.dp.px.toFloat()
                                ),
                                radius = 9.dp.px.toFloat()
                            ),
                            shape = CircleShape
                        )
                        .zIndex(1f)
                )
                Text(
                    text = info.name.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)
                )
            }
        }
        status.photo?.let { photo ->
            AsyncImage(
                model = photo.let(api::url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = if (status.statusInfo != null) 2.pad else 0.pad)
                    .requiredSize(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable {
                        onShowPhoto()
                    }
            )
        }
        status.note?.let { note ->
            SelectionContainer {
                LinkifyText(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = if (status.photo != null || status.statusInfo != null) 2.pad else 0.pad)
                )
            }
        }
        Text(
            text = status.createdAt!!.timeAgo(),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = .5f.pad)
        )
    }
}
