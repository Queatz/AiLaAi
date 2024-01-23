package components

import androidx.compose.runtime.*
import app.AppNavigation
import app.appNav
import app.softwork.routingcompose.Router
import com.queatz.db.Card
import kotlinx.coroutines.launch
import notEmpty
import stories.StoryContents
import stories.asStoryContents

@Composable
fun Content(content: String?) {
    val storyContent by remember(content) { mutableStateOf(content?.asStoryContents()) }
    val scope = rememberCoroutineScope()

    if (storyContent?.notEmpty != null) {
        StoryContents(
            storyContent!!,
            onGroupClick = {
                scope.launch {
                    appNav.navigate(AppNavigation.Group(it.group!!.id!!, it))
                }
            }
        )
    }
}
