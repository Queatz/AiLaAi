import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var showSearch by remember(group.group?.id) { mutableStateOf(false) }

    application.background(group.group?.background?.let { "$baseUrl$it" })
    application.effects(group.group?.config?.effects?.let { json.decodeFromString(it) })

    LaunchedEffect(group.group?.id) {
        group.group?.id?.let { groupId ->
            // Mark group as read
            api.group(groupId) {}
        }
    }

    if (showCards) {
        GroupCards(group = group)
    } else {
        GroupMessages(
            group = group,
            showSearch = showSearch,
            onShowSearch = {
                showSearch = it
            }
        )
    }

    GroupTopBar(
        group = group,
        onGroupUpdated = onGroupUpdated,
        onGroupGone = onGroupGone,
        showCards = showCards,
        onShowCards = {
            showCards = !showCards
        },
        onSearch = {
            showSearch = !showSearch
        }
    )
}
