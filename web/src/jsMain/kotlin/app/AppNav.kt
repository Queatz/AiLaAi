package app

import com.queatz.db.GroupExtended
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

val appNav = AppNav()

sealed interface AppNavigation {
    class Group(val group: String, val groupExtended: GroupExtended? = null) : AppNavigation
}

class AppNav {
    private val _navigate = MutableSharedFlow<AppNavigation>()

    val navigate = _navigate.asSharedFlow()

    suspend fun navigate(destination: AppNavigation) {
        _navigate.emit(destination)
    }
}
