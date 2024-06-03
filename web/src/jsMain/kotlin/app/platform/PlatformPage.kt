package app.platform

import androidx.compose.runtime.Composable
import app.FullPageLayout
import app.components.Empty
import org.jetbrains.compose.web.dom.Text

@Composable
fun PlatformPage(selected: PlatformNav) {
    FullPageLayout {
        when (selected) {
            is PlatformNav.Health -> HealthPlatformPage()
            is PlatformNav.Config -> ConfigPlatformPage()
            is PlatformNav.Reports -> ReportsPlatformPage()
            is PlatformNav.Stats -> StatsPlatformPage()
            is PlatformNav.Crashes -> CrashesPlatformPage()
            else -> Empty { Text("Not yet implemented.") }
        }
    }
}
