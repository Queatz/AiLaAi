package components

import MapView
import Styles
import androidx.compose.runtime.Composable
import app.AppPage
import appString
import application
import org.jetbrains.compose.web.dom.Div

@Composable
fun MainPage(tabId: String? = null) {
    if (application.me.value == null) {
        Div({
            classes(Styles.mainContainer)
        }) {
            MapView {
                AppHeader(
                    title = appString { appName },
                    background = false,
                    showDownloadApp = false,
                    showMe = false
                )
            }
            AppFooter()
        }
    } else {
        AppPage(tabId)
    }
}
