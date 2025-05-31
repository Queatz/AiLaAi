package app

import application
import app.components.checkNavigationAllowed
import com.queatz.db.Card
import com.queatz.db.GameScene as GameSceneModel
import com.queatz.db.GroupExtended
import com.queatz.db.Script
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

val appNav = AppNav()

sealed interface AppNavigation {
    data class Nav(val nav: NavPage) : AppNavigation
    data class Group(val id: String, val groupExtended: GroupExtended? = null) : AppNavigation
    data class Page(val id: String, val card: Card? = null) : AppNavigation
    data class GameScene(val id: String, val gameScene: GameSceneModel? = null) : AppNavigation
    data class Script(val id: String, val script: com.queatz.db.Script? = null) : AppNavigation
    data object ExploreScripts : AppNavigation
}

class AppNav {
    private val _navigate = MutableSharedFlow<AppNavigation>(replay = 1)
    private val _route = MutableSharedFlow<String>()

    val navigate = _navigate.asSharedFlow()
    val route = _route.asSharedFlow()

    suspend fun navigate(destination: AppNavigation) {
        if (application.me.value == null) {
            _route.emit("/signin")
            return
        }

        // Check if navigation is allowed (will show confirmation dialog if needed)
        val canProceed = checkNavigationAllowed()

        // Only proceed with navigation if allowed
        if (canProceed) {
            _navigate.emit(destination)
            _route.emit("/")
        }
    }
}
