package app

import application
import com.queatz.db.GroupExtended
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

val appNav = AppNav()

sealed interface AppNavigation {
    class Group(val group: String, val groupExtended: GroupExtended? = null) : AppNavigation
}

class AppNav {
    private val _navigate = MutableSharedFlow<AppNavigation>(replay = 1)
    private val _route = MutableSharedFlow<String>()

    val navigate = _navigate.asSharedFlow()
    val route = _route.asSharedFlow()

    suspend fun navigate(destination: AppNavigation) {
        if (application.me.value == null) {
            _route.emit("/signin")
        } else {
            _navigate.emit(destination)
            _route.emit("/")
        }
    }
}
