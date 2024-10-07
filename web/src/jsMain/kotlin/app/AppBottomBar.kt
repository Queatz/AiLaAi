package app

import Styles
import androidx.compose.runtime.Composable
import appString
import components.IconButton
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.dom.Div

@Composable
fun AppBottomBar(nav: NavPage, onNavClick: (NavPage) -> Unit) {
    Div({
        classes(AppStyles.bottomBar)
        style {
            flexShrink(0)
        }
    }) {
        IconButton("forum", appString { groups }, styles = {
            if (nav == NavPage.Groups) {
                backgroundColor(Styles.colors.primary)
                color(Color.white)
            }
        }) {
            onNavClick(NavPage.Groups)
        }
        IconButton("newspaper", appString { posts }, styles = {
            if (nav == NavPage.Stories) {
                backgroundColor(Styles.colors.primary)
                color(Color.white)
            }
        }) {
            onNavClick(NavPage.Stories)
        }
        IconButton("map", appString { map }, styles = {
            if (nav == NavPage.Cards) {
                backgroundColor(Styles.colors.primary)
                color(Color.white)
            }
        }) {
            onNavClick(NavPage.Cards)
        }
        IconButton("calendar_month", appString { reminders }, styles = {
            if (nav == NavPage.Schedule) {
                backgroundColor(Styles.colors.primary)
                color(Color.white)
            }
        }) {
            onNavClick(NavPage.Schedule)
        }
//        IconButton("rocket", "Inventory", styles = {
//            if (nav == NavPage.Schedule) {
//                backgroundColor(Styles.colors.primary)
//                color(Color.white)
//            }
//        }) {
//            onNavClick(NavPage.Schedule)
//        }
    }
}
