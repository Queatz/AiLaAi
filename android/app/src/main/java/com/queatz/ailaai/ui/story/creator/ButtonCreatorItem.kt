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

fun LazyGridScope.buttonCreatorItem(creatorScope: CreatorScope<StoryContent.Button>) = with(creatorScope) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var showButtonMenu by rememberStateOf(false)

        if (showButtonMenu) {
            Menu({
                showButtonMenu = false
            }) {
                menuItem(stringResource(R.string.remove)) {
                    showButtonMenu = false
                    remove(partIndex)
                }
            }
        }

        Button(
            onClick = {
                showButtonMenu = true
            }
        ) {
            Text(part.text)
        }
    }
}
