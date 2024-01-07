package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun OpenSourcePage() {
    Div {
        H1 {
            Text("Ai là ai is Open Source")
        }
        A("https://github.com/Queatz/AiLaAi") {
            Text("Go to GitHub")
        }
    }
}
