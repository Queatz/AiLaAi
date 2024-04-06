package app.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.runScript
import application
import com.queatz.db.RunScriptBody
import com.queatz.db.ScriptResult
import com.queatz.db.Widget
import com.queatz.widgets.widgets.ScriptData
import json
import kotlinx.coroutines.launch
import notEmpty
import stories.StoryContents
import widget

@Composable
fun ScriptWidget(widgetId: String) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var widget by remember(widgetId) {
        mutableStateOf<Widget?>(null)
    }

    var data by remember(widgetId) {
        mutableStateOf<ScriptData?>(null)
    }

    var scriptResult by remember(widgetId) {
        mutableStateOf<ScriptResult?>(null)
    }

    LaunchedEffect(widgetId) {
        // todo loading
        api.widget(widgetId) {
            it.data ?: return@widget
            widget = it
            data = json.decodeFromString<ScriptData>(it.data!!)
        }
    }

    LaunchedEffect(data) {
        data?.let { data ->
            api.runScript(data.script!!, RunScriptBody(data.data)) {
                scriptResult = it
            }
        }
    }

    scriptResult?.content?.notEmpty?.let { content ->
        StoryContents(
            storyContent = content,
            onGroupClick = {},
            onButtonClick = { script, data ->
                scope.launch {
                    api.runScript(script, RunScriptBody(data)) {
                        scriptResult = it
                    }
                }
            }
        )
    }
}
