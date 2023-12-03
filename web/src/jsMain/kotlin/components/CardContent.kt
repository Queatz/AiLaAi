package components

import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import com.queatz.db.Card
import notEmpty
import stories.StoryContents
import stories.asStoryContents

@Composable
fun CardContent(card: Card) {
    if (card.content != null) {
        var storyContent by remember(card.content) { mutableStateOf(card.content?.asStoryContents()) }

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
}
