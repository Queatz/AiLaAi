package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.nullIfBlank

@Composable
fun ProfileImage(
    photo: String?,
    name: String?,
    padding: PaddingValues = PaddingValues(0.dp),
    size: Dp = 32.dp,
    onClick: () -> Unit,
) {
    if (photo?.nullIfBlank == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    onClick()
                }
        ) {
            Text(
                name?.take(1) ?: "",
                style = MaterialTheme.typography.titleMedium.let {
                    if (size <= 18.dp) {
                        it.copy(
                            lineHeight = 12.sp,
                            fontSize = 12.sp
                        )
                    } else {
                        it
                    }
                }
            )
        }
    } else {
        AsyncImage(
            model = photo.let { api.url(it) },
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    onClick()
                }
        )
    }
}
