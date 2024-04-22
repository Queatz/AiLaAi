package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.extensions.launchUrl
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.UrlAttachment

@Composable
fun UrlPreview(attachment: UrlAttachment) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(.5f.pad),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .clickable {
                attachment.url!!.launchUrl(context)
            }
            .padding(bottom = 1.pad)
            .widthIn(max = 480.dp)
    ) {
        attachment.image?.notBlank?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            )
        }
        attachment.title?.notBlank?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 1.5f.pad)
            )
        }
        attachment.description?.notBlank?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(horizontal = 1.5f.pad)
            )
        }
    }
}
