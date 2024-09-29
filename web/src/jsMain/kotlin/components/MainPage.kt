import androidx.compose.runtime.Composable
import app.AppPage
import components.AppHeader
import org.jetbrains.compose.web.dom.Div

@Composable
fun MainPage() {
    if (application.me.value == null) {
        Div({
            classes(Styles.mainContainer)
        }) {
            MapView {
                AppHeader(appString { appName }, background = false, showDownloadApp = true)
            }
        }
    } else {
        AppPage()
    }
}
