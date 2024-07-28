package app.bots

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import application
import com.queatz.db.Bot
import components.IconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

suspend fun groupBotDialog(
    scope: CoroutineScope,
    bot: Bot,
    onOpenBot: (Bot) -> Unit,
    onBotRemoved: (Bot) -> Unit,
    onEditBot: (Bot) -> Unit
) {
    dialog(
        bot.name,
        cancelButton = null,
        confirmButton = application.appString { close },
        actions = {
            val me = application.me.collectAsState()
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }

            menuTarget?.let {
                Menu({ menuTarget = null }, it) {
                    // todo: translate
                    item("Edit") {
                        onEditBot(bot)
                    }

                    // todo: if I am a group host
                    if (true) {
                        // todo: translate
                        item("Remove") {
                            onBotRemoved(bot)
                        }
                        // todo: translate
                        item("Go to bot") {
                            onOpenBot(bot)
                        }
                    }
                }
            }

            // todo: translate
            IconButton("more_vert", "Menu") {
                menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
            }
        }
    ) {
        bot.description?.notBlank?.let {
            Text(it)
        }
    }
}
