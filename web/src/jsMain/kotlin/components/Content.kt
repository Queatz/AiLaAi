package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppNavigation
import app.ailaai.api.runScript
import app.appNav
import com.queatz.db.RunScriptBody
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch
import stories.StoryContents
import stories.asStoryContents

@Composable
fun Content(
    content: String?,
    onCardClick: ((cardId: String, openInNewWindow: Boolean) -> Unit)? = null,
    actions: @Composable (index: Int, part: StoryContent) -> Unit = { _, _ -> },
    formReloadKey: Int = 0,
    cardId: String? = null, // for creating widgets
    editable: Boolean = false,
    onEdited: ((index: Int, part: StoryContent) -> Unit)? = null,
    onReorder: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onSave: ((List<StoryContent>) -> Unit)? = null,
) {
    var storyContent by remember(content) { mutableStateOf(content?.asStoryContents()) }
    val scope = rememberCoroutineScope()

    StoryContents(
        content = storyContent ?: emptyList(),
        onCardClick = onCardClick,
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
                storyContent = it.content
            }
        },
        actions = actions,
        cardId = cardId,
        formReloadKey = formReloadKey,
        editable = editable,
        onEdited = onEdited,
        onReorder = onReorder,
        onSave = onSave
    )
}
