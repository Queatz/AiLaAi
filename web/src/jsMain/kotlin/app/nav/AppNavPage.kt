package app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.AppNavigation
import app.NavPage
import app.appNav
import app.group.FeaturePreview
import appString
import application
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div

@Composable
fun AppNavPage() {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.FlexStart)
        }
    }) {
        Div({
            style {
                width(100.percent)
            }
        }) {
            NavTopBar(
                me = me,
                title = appString { apps },
                onProfileClick = {
                    scope.launch {
                        appNav.navigate(AppNavigation.Nav(NavPage.Profile))
                    }
                }
            )
            FeaturePreview(
                pages = listOf(NavPage.Scenes, NavPage.Scripts),
                center = false,
                create = false
            )
        }
    }
}
