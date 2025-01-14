package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*

@Composable
fun OpenSourcePage() {
    Div {
        H1 {
            Text("Open Source")
        }
        Div {
            Text("Hi Town runs the latest version of Ai là ai.")
        }
        A("https://github.com/Queatz/AiLaAi", {
            target(ATarget.Blank)
        }) {
            Text("Go to GitHub")
        }
    }
}
