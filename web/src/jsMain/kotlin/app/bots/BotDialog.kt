package app.bots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.dialog.dialog
import app.menu.Menu
import application
import com.queatz.db.Bot
import components.IconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r

suspend fun botDialog(
    scope: CoroutineScope,
    bot: Bot
) {
    val secret = MutableStateFlow("")

    val result = dialog(
        title = bot.name!!,
        confirmButton = application.appString { close },
        cancelButton = null,
        actions = {
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }

            menuTarget?.let {
                Menu({ menuTarget = null }, it) {
                    // todo: translate
                    item("Secret") {
                        scope.launch {
                            val result = botSecretDialog(secret.value)

                            if (result != null) {
                                secret.value = result
                            }
                        }
                    }
                    item(application.appString { delete }) {
                        scope.launch {
                            val result = dialog(
                                // todo: translate
                                "Delete this bot?",
                                // todo: translate
                                confirmButton = "Yes, delete"
                            ) {
                                // todo: translate
                                Text("This will remove the bot from any groups it is currently added to.")
                            }

                            if (result == true) {
                                // todo api call
                            }
                        }
                    }
                }
            }

            // todo: translate
            IconButton("more_vert", "More options") {
                menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
            }
        }
    ) {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
            }
        }) {
            bot.description?.notBlank?.let {
                Text(it)
            }
            Pre({
                codeBlock()
            }) {
                Text(bot.url ?: "")
            }
        }
    }
}
