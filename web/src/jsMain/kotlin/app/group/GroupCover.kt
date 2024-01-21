package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.AppNavigation
import app.appNav
import com.queatz.db.GroupExtended
import kotlinx.coroutines.launch

@Composable
fun GroupCover(group: GroupExtended) {
    val scope = rememberCoroutineScope()

    GroupItem(
        group,
        onBackground = true,
        coverPhoto = true,
        onSelected = {
            scope.launch {
                appNav.navigate(AppNavigation.Group(group.group!!.id!!, group))
            }
        },
        info = GroupInfo.Members
    )
}
