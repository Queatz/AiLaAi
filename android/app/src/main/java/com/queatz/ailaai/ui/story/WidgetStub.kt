package com.queatz.ailaai.ui.story

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.ailaai.api.script
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ScriptData
import widget

@Composable
fun WidgetStub(
    part: StoryContent.Widget,
    onClick: () -> Unit = {}
) {
    var description by rememberStateOf("")
    val noData = stringResource(R.string.no_data)

    LaunchedEffect(part) {
        when (part.widget) {
            Widgets.Script -> {
                api.widget(part.id) { widget ->
                    try {
                        json.decodeFromString<ScriptData>(widget.data!!).let { data ->
                            api.script(data.script!!) { script ->
                                description = "${script.name} (${data.data?.notBlank ?: noData})"
                            }
                        }
                    } catch (_: Throwable) {
                        // Ignored
                    }
                }
            }
            else -> Unit
        }
    }

    Stub(bulletedString(part.widget.stringResource, description.notBlank)) {
        onClick()
    }
}
