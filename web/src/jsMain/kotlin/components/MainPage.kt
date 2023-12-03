import androidx.compose.runtime.Composable
import components.AppFooter
import components.AppHeader
import app.AppPage
import components.HomePage

@Composable
fun MainPage() {
    if (application.me.value == null) {
        AppHeader(appString { appName }, showMenu = true)
        HomePage()
        AppFooter()
    } else {
        AppPage()
    }
}
