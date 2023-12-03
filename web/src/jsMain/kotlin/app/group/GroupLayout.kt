import androidx.compose.runtime.*
import app.ailaai.api.group
import app.group.GroupTopBar
import com.queatz.db.GroupExtended

@Composable
fun GroupLayout(
    group: GroupExtended,
    onGroupUpdated: () -> Unit,
    onGroupGone: () -> Unit
) {
     var showCards by remember(group.group?.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(group.group?.id) {
        group.group?.id?.let { groupId ->
            // Mark group as read
            api.group(groupId) {}
        }
    }

    if (showCards) {
        GroupCards(group)
    } else {
        GroupMessages(group)
    }

    GroupTopBar(
        group,
        onGroupUpdated = onGroupUpdated,
        onGroupGone = onGroupGone,
        showCards = showCards,
        onShowCards = {
            showCards = !showCards
        }
    )
}
