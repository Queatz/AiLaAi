package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.ailaai.api.group
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.LoadingText
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.db.GroupExtended
import com.queatz.db.StoryContent

fun LazyGridScope.groupsItem(content: StoryContent.Groups) {
    items(content.groups, span = { GridItemSpan(maxLineSpan) }) { groupId ->
        DisableSelection {
            var group by remember { mutableStateOf<GroupExtended?>(null) }

            LaunchedEffect(groupId) {
                api.group(groupId) { group = it }
            }

            LoadingText(group != null, stringResource(R.string.loading_group)) {
                ContactItem(
                    SearchResult.Group(group!!),
                    onChange = {},
                    info = GroupInfo.Members,
                    coverPhoto = content.coverPhotos
                )
            }
        }
    }
}
