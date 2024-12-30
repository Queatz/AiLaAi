package app.scripts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.PageTopBar
import app.ailaai.api.updateScript
import app.components.EditField
import bulletedString
import com.queatz.db.Script
import components.Loading
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun ScriptsPage(
    nav: ScriptsNav,
    onUpdate: (Script) -> Unit
) {
    when (nav) {
        is ScriptsNav.None -> {}
        is ScriptsNav.Script -> {
            var script by remember(nav.script) { mutableStateOf(nav.script) }
            var isLoading by remember { mutableStateOf(false) }

            if (isLoading) {
                Loading()
            } else {
                Div({
                    style {
                        flex(1)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        overflowY("auto")
                        overflowX("hidden")
                    }
                }) {
                    EditField(
                        script.source.orEmpty(),
                        styles = {
                            margin(1.r, 1.r, 0.r, 1.r)
                        }
                    ) {
                        var success = false
                        api.updateScript(script.id!!, Script(source = it)) {
                            success = true
                            onUpdate(it)
                        }

                        success
                    }
                }
                PageTopBar(
                    // todo: translate
                    title = script.name ?: "New script",
                    description = bulletedString(
                        script.categories?.firstOrNull(),
                        script.description
                    )
                )
            }
        }
    }
}
