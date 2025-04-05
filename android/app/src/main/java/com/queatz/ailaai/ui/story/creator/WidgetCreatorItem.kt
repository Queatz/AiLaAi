package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.story.WidgetStub
import com.queatz.ailaai.ui.widget.form.EditFormDialog
import com.queatz.db.StoryContent
import com.queatz.db.Widget
import com.queatz.widgets.Widgets
import kotlinx.coroutines.launch
import updateWidget
import widget

fun LazyGridScope.widgetCreatorItem(
    creatorScope: CreatorScope<StoryContent.Widget>
) = with(creatorScope) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val scope = rememberCoroutineScope()
        var showWidgetMenu by rememberStateOf(false)
        var showEdit by rememberStateOf(false)

        if (showEdit) {
            when (part.widget) {
                Widgets.Form -> {
                    var widget by rememberStateOf<Widget?>(null)

                    scope.launch {
                        api.widget(part.id) {
                            widget = it
                        }
                    }

                    widget?.let { widget ->
                        EditFormDialog(
                            onDismissRequest = {
                                showEdit = false
                            },
                            initialFormData = json.decodeFromString(widget.data!!),
                        ) { data ->
                            api.updateWidget(
                                id = part.id,
                                widget = Widget(data = json.encodeToString(data))
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        if (showWidgetMenu) {
            Menu({
                showWidgetMenu = false
            }) {
                when (part.widget) {
                    Widgets.Form -> {
                        menuItem(stringResource(R.string.edit)) {
                            showWidgetMenu = false
                            showEdit = true
                        }
                    }
                    else -> Unit
                }
                menuItem(stringResource(R.string.remove)) {
                    showWidgetMenu = false
                    remove(partIndex)
                }
            }
        }

        WidgetStub(
            part = part
        ) {
            showWidgetMenu = true
        }
    }
}
