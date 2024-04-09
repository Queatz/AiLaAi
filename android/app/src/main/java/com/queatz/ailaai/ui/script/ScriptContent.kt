package com.queatz.ailaai.ui.script

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ailaai.api.runScript
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.story.StoryContents
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.RunScriptBody
import com.queatz.db.StoryContent
import com.queatz.db.Widget
import com.queatz.widgets.widgets.ScriptData
import kotlinx.coroutines.launch
import widget

fun LazyGridScope.ScriptContent(widgetId: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val me = me
        val scope = rememberCoroutineScope()
        var scriptUi by rememberStateOf(emptyList<StoryContent>())

        var widget by remember(widgetId) {
            mutableStateOf<Widget?>(null)
        }

        var data by remember(widgetId) {
            mutableStateOf<ScriptData?>(null)
        }

        var isLoading by remember(widgetId) {
            mutableStateOf(true)
        }

        LaunchedEffect(widgetId) {
            // todo loading
            api.widget(widgetId) {
                it.data ?: return@widget
                widget = it
                data = json.decodeFromString<ScriptData>(it.data!!)
            }
        }

        LaunchedEffect(widgetId, data) {
            api.runScript(
                data?.script ?: return@LaunchedEffect,
                RunScriptBody(data?.data)
            ) {
                scriptUi = it.content ?: emptyList()
            }
            isLoading = false
        }

        if (isLoading) {
          Loading(
              modifier = Modifier
                  .padding(vertical = 1.pad)
          )
        } else if (scriptUi.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .heightIn(max = 512.dp)
            ) {
                StoryContents(
                    null,
                    scriptUi,
                    rememberLazyGridState(),
                    fade = true,
                    horizontalPadding = 0.dp,
                    onButtonClick = { script, data ->
                        scope.launch {
                            api.runScript(
                                script,
                                RunScriptBody(data)
                            ) {
                                scriptUi = it.content ?: emptyList()
                            }
                        }
                    }
                )
            }
        }
    }
}
