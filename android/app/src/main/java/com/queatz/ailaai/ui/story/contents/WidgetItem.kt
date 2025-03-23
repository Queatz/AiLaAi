package com.queatz.ailaai.ui.story.contents

import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import com.queatz.ailaai.ui.script.ScriptContent
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.story.Stub
import com.queatz.ailaai.ui.story.stringResource
import com.queatz.ailaai.ui.widget.WebWidgetContent
import com.queatz.ailaai.ui.widget.shop.ShopContent
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets

fun LazyGridScope.widgetItem(
    content: StoryContent.Widget,
    source: StorySource?,
    fullscreenWebView: (Pair<View, WebChromeClient.CustomViewCallback>?) -> Unit,
    showOpenWidgetDialog: (Boolean) -> Unit,
) {
    when (content.widget) {
        Widgets.Script -> {
            ScriptContent(content.id)
        }

        Widgets.Web -> {
            WebWidgetContent(content.id, fullscreenWebView)
        }

        Widgets.Shop -> {
            ShopContent(content.id)
        }

        else -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DisableSelection {
                    Stub(content.widget.stringResource) {
                        if (source is StorySource.Card) {
                            showOpenWidgetDialog(true)
                        }
                    }
                }
            }
        }
    }
}
