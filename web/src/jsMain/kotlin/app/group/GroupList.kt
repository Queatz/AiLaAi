package app.group

import androidx.compose.runtime.Composable
import com.queatz.db.GroupExtended
import org.jetbrains.compose.web.css.marginBottom
import r

@Composable
fun GroupList(
    groups: List<GroupExtended>,
    onSelected: (GroupExtended) -> Unit
) {
    groups.forEach {
        GroupItem(
            it,
            selectable = false,
            onSelected = {
                onSelected(it)
            },
            info = GroupInfo.Members,
            styles = {
                marginBottom(1.r)
            }
        )
    }
}
