package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.AppNavigation
import app.appNav
import app.softwork.routingcompose.Router
import application
import com.queatz.db.GroupExtended
import kotlinx.coroutines.launch

@Composable
fun GroupCover(group: GroupExtended) {
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    val router = Router.current

    GroupItem(
        group,
        onBackground = true,
        coverPhoto = true,
        onSelected = {
            scope.launch {
                if (me == null) {
                    router.navigate("/signin")
                } else {
                    appNav.navigate(AppNavigation.Group(group.group!!.id!!, group))
                    router.navigate("/")
                }
            }
        },
        info = GroupInfo.Members
    )
}
