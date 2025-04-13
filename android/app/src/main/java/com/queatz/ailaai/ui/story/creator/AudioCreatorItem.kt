package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.queatz.ailaai.data.api
import com.queatz.ailaai.ui.components.Audio
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.StoryContent

fun LazyGridScope.audioCreatorItem(creatorScope: CreatorScope<StoryContent.Audio>) = with(creatorScope) {
    item(
        span = { GridItemSpan(maxLineSpan) },
        key = creatorScope.id
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                Audio(
                    api.url(part.audio),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
                IconButton(
                    onClick = {
                        remove(partIndex)
                    }
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
