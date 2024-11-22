package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.story.WidgetStub
import com.queatz.db.StoryContent

fun LazyGridScope.widgetCreatorItem(
    creatorScope: CreatorScope<StoryContent.Widget>
) = with(creatorScope) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var showWidgetMenu by rememberStateOf(false)

        if (showWidgetMenu) {
            Menu({
                showWidgetMenu = false
            }) {
                menuItem(stringResource(R.string.remove)) {
                    showWidgetMenu = false
                    remove(partIndex)
                }
                // todo edit (same-ish as AddWidgetDialog)
            }
        }

        WidgetStub(
            part
        ) {
            showWidgetMenu = true
        }
    }
}
