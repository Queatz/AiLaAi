package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.AppNavigation
import app.appNav
import com.queatz.db.Card
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch
import notEmpty
import stories.StoryContents
import stories.asStoryContents

@Composable
fun Content(
    content: String?,
    onCardClick: ((cardId: String, openInNewWindow: Boolean) -> Unit)? = null,
    actions: @Composable (index: Int, part: StoryContent) -> Unit = { _, _ -> },
    formReloadKey: Int = 0,
) {
    val storyContent by remember(content) { mutableStateOf(content?.asStoryContents()) }
    val scope = rememberCoroutineScope()

    if (storyContent?.notEmpty != null) {
        StoryContents(
            content = storyContent!!,
            onCardClick = onCardClick,
            onGroupClick = {
                scope.launch {
                    appNav.navigate(AppNavigation.Group(it.group!!.id!!, it))
                }
            },
            actions = actions,
            formReloadKey = formReloadKey
        )
    }
}
