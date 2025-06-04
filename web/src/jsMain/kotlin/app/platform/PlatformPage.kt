package app.platform

import FeedbackPlatformPage
import androidx.compose.runtime.Composable
import app.FullPageLayout
import app.components.Empty
import org.jetbrains.compose.web.dom.Text

@Composable
fun PlatformPage(selected: PlatformNav) {
    FullPageLayout {
        when (selected) {
            is PlatformNav.None -> {}
            is PlatformNav.Accounts -> AccountsPlatformPage()
            is PlatformNav.People -> PeoplePlatformPage()
            is PlatformNav.Health -> HealthPlatformPage()
            is PlatformNav.Config -> ConfigPlatformPage()
            is PlatformNav.Stats -> StatsPlatformPage()
            is PlatformNav.Reports -> ReportsPlatformPage()
            is PlatformNav.Feedback -> FeedbackPlatformPage()
            is PlatformNav.Crashes -> CrashesPlatformPage()
            else -> Empty { Text("Not yet implemented.") }
        }
    }
}
