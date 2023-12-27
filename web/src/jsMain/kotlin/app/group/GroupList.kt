package app.group

import androidx.compose.runtime.Composable
import com.queatz.db.GroupExtended
import org.jetbrains.compose.web.css.marginBottom
import r

@Composable
fun GroupList(
    groups: List<GroupExtended>,
    onSurface: Boolean = false,
    onSelected: (GroupExtended) -> Unit
) {
    groups.forEachIndexed { index, it ->
        GroupItem(
            it,
            selectable = onSurface,
            onSurface = onSurface,
            onSelected = {
                onSelected(it)
            },
            info = GroupInfo.Members,
            styles = {
                if (index != groups.lastIndex) {
                    marginBottom(1.r)
                }
            }
        )
    }
}
