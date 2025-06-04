package app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.nav.NavMenu
import app.nav.NavMenuItem
import app.nav.NavTopBar
import appString
import application
import org.jetbrains.compose.web.css.Color

sealed class PlatformNav {
    data object None : PlatformNav()
    data object Accounts : PlatformNav()
    data object People : PlatformNav()
    data object Reviews : PlatformNav()
    data object Reports : PlatformNav()
    data object Stats : PlatformNav()
    data object Feedback : PlatformNav()
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
        NavMenuItem("people", "Accounts", selected = selected == PlatformNav.Accounts, iconColor = Color("#60BFBF")) {
        onSelected(PlatformNav.Accounts)
        }
        // todo: translate
        NavMenuItem("person", "People", selected = selected == PlatformNav.People, iconColor = Color("#60BF60")) {
        onSelected(PlatformNav.People)
        }
        // todo: translate
        NavMenuItem("favorite", "Health", selected = selected == PlatformNav.Health, iconColor = Color("#FF69B4")) {
        onSelected(PlatformNav.Health)
        }
        // todo: translate
        NavMenuItem("settings", "Config", selected = selected == PlatformNav.Config, iconColor = Color("#808080")) {
        onSelected(PlatformNav.Config)
        }
        // todo: translate
        NavMenuItem("bar_chart", "Stats", selected = selected == PlatformNav.Stats, iconColor = Color("#60BF60")) {
        onSelected(PlatformNav.Stats)
        }
        // todo: translate
        NavMenuItem("report", "Reports", selected = selected == PlatformNav.Reports, iconColor = Color("#6060BF")) {
            onSelected(PlatformNav.Reports)
        }
        // todo: translate
        NavMenuItem("feedback", "Feedback", selected = selected == PlatformNav.Feedback, iconColor = Color("#BF9060")) {
        onSelected(PlatformNav.Feedback)
        }
        // todo: translate
        NavMenuItem("bug_report", "Crashes", selected = selected == PlatformNav.Crashes, iconColor = Color("#BF6060")) {
        onSelected(PlatformNav.Crashes)
        }
    }
}
