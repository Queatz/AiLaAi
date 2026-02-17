package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.runScript
import app.ailaai.api.script
import com.queatz.db.RunScriptBody
import com.queatz.db.ScriptResult
import org.jetbrains.compose.web.dom.Div
import stories.StoryContents
import com.queatz.db.GroupContent as GroupContentModel

@Composable
fun GroupContentScript(
    content: GroupContentModel.Script,
    setTitle: (String?) -> Unit
) {
    var scriptResult by remember { mutableStateOf<ScriptResult?>(null) }
    var isRunningScript by remember { mutableStateOf(false) }
    var scriptResultKey by remember { mutableStateOf(0) }

    val runScript: suspend (String, String?, Map<String, String?>, Boolean?) -> Unit =
        { id, data, input, useCache ->
            isRunningScript = true
            try {
                api.runScript(id, RunScriptBody(data = data, input = input, useCache = useCache)) {
                    scriptResult = it
                    scriptResultKey++
                }
            } finally {
                isRunningScript = false
            }
        }

    LaunchedEffect(content.scriptId, content.data) {
        content.scriptId?.let { scriptId ->
            api.script(scriptId) {
                setTitle(it.name)
            }
            runScript(scriptId, content.data, emptyMap(), true)
        }
    }

    scriptResult?.content?.let {
        Div({
            classes(Styles.cardContent)
        }) {
            StoryContents(
                content = it,
                key = scriptResultKey,
                onButtonClick = { script, data, input ->
                    runScript(
                        script,
                        data,
                        input,
                        true
                    )
                },
            )
        }
    }
}
