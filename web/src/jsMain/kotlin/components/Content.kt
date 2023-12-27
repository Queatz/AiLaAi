package components

import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import com.queatz.db.Card
import notEmpty
import stories.StoryContents
import stories.asStoryContents

@Composable
fun Content(content: String?) {
    var storyContent by remember(content) { mutableStateOf(content?.asStoryContents()) }

    val router = Router.current

    if (storyContent?.notEmpty != null) {
        StoryContents(
            storyContent!!,
            onGroupClick = {
                // todo if signed in, go to group
                router.navigate("/signin")
            }
        )
    }
}
