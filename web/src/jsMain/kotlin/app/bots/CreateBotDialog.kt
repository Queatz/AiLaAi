package app.bots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.dialog.inputDialog
import app.menu.Menu
import application
import components.IconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

suspend fun createBotDialog(
    scope: CoroutineScope,
    onBotHelp: () -> Unit
) {
    val secret = MutableStateFlow("")

    val url = inputDialog(
        title = application.appString { createBot },
        placeholder = "https://",
        confirmButton = application.appString { create },
        extraButtons = {
            // todo: translate
            IconButton("help", "Bot specifications") {
                onBotHelp()
            }
        },
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
                }
            }

            // todo: translate
            IconButton("more_vert", "More options") {
                menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
            }
        }
    )

    if (url != null) {
//        api.createBot()
    }
}
