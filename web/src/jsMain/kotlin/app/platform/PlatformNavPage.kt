package app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.nav.NavMenu
import app.nav.NavMenuItem
import app.nav.NavTopBar
import appString
import application


sealed class PlatformNav {
    data object None : PlatformNav()
    data object Reviews : PlatformNav()
    data object Reports : PlatformNav()
    data object Stats : PlatformNav()
    data object Crashes : PlatformNav()
    data object Config : PlatformNav()
    data object Health : PlatformNav()
}

@Composable
fun PlatformNavPage(
    onProfileClick: () -> Unit,
    selected: PlatformNav,
    onSelected: (PlatformNav) -> Unit
) {
    val me by application.me.collectAsState()

    NavTopBar(me, appString { platform }, onProfileClick = onProfileClick)

    NavMenu {
        // todo: translate
        NavMenuItem(null, "Health", selected = selected == PlatformNav.Health) {
            onSelected(PlatformNav.Health)
        }
        // todo: translate
        NavMenuItem(null, "Config", selected = selected == PlatformNav.Config) {
            onSelected(PlatformNav.Config)
        }
        // todo: translate
        NavMenuItem(null, "Reports", selected = selected == PlatformNav.Reports) {
            onSelected(PlatformNav.Reports)
        }
        // todo: translate
        NavMenuItem(null, "Stats", selected = selected == PlatformNav.Stats) {
            onSelected(PlatformNav.Stats)
        }
        // todo: translate
        NavMenuItem(null, "Crashes", selected = selected == PlatformNav.Crashes) {
            onSelected(PlatformNav.Crashes)
        }
    }
}
