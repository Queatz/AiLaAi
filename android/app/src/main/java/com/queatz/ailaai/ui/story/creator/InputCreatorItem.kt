package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.db.StoryContent

fun LazyGridScope.inputCreatorItem(creatorScope: CreatorScope<StoryContent.Input>) = with(creatorScope) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var showInputMenu by rememberStateOf(false)

        if (showInputMenu) {
            Menu({
                showInputMenu = false
            }) {
                menuItem(stringResource(R.string.remove)) {
                    showInputMenu = false
                    remove(partIndex)
                }
            }
        }

        Button(
            onClick = {
                showInputMenu = true
            }
        ) {
            Text(part.key)
        }
    }
}
