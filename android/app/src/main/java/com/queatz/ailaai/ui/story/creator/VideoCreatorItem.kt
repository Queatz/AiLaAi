package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Video
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.db.StoryContent

fun LazyGridScope.videoCreatorItem(creatorScope: CreatorScope<StoryContent.Video>) = with(creatorScope) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var showMenu by rememberStateOf(false)
        if (showMenu) {
            Menu({
                showMenu = false
            }) {
                menuItem(stringResource(R.string.remove)) {
                    showMenu = false
                    remove(partIndex)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Video(
                url = part.video.let(api::url),
                showController = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .aspectRatio(1.5f)
                    .weight(1f)
            )
            IconButton(
                onClick = {
                    showMenu = true
                }
            ) {
                Icon(Icons.Default.MoreVert, null)
            }
        }
    }
}
