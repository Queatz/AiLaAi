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
import app.AppNavigation
import app.ailaai.api.runScript
import app.appNav
import application
import com.queatz.db.RunScriptBody
import com.queatz.db.ScriptResult
import com.queatz.db.Widget
import com.queatz.widgets.widgets.ScriptData
import components.Loading
import json
import kotlinx.coroutines.launch
import notEmpty
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r
import stories.StoryContents
import widget
import kotlin.random.Random.Default.nextInt

@Composable
fun ScriptWidget(widgetId: String) {
    val me by application.me.collectAsState()
    val appNav = appNav
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

    var scriptResultKey by remember(widgetId) {
        mutableStateOf(0)
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
                scriptResultKey = nextInt()
            }
        }
    }

    scriptResult?.content?.notEmpty?.let { content ->
        StoryContents(
            content = content,
            key = scriptResultKey,
            onGroupClick = {
                scope.launch {
                    appNav.navigate(AppNavigation.Group(it.group!!.id!!, it))
                }
            },
            onButtonClick = { script, data, input ->
                api.runScript(
                    id = script,
                    data = RunScriptBody(
                        data = data,
                        input = input
                    )
                ) {
                    scriptResult = it
                    scriptResultKey = nextInt()
                }
            }
        )
    } ?: run {
        Div({
            style {
                width(100.percent)
                padding(1.r)
            }
        }) {
            Loading()
        }
        // todo error state
    }
}
