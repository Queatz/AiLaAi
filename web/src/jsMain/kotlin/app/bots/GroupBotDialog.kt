package app.bots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.deleteGroupBot
import app.ailaai.api.updateGroupBot
import app.dialog.dialog
import app.menu.Menu
import application
import com.queatz.db.Bot
import com.queatz.db.GroupBot
import com.queatz.db.GroupExtended
import com.queatz.db.MemberAndPerson
import components.IconButton
import components.Switch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

suspend fun groupBotDialog(
    scope: CoroutineScope,
    bot: Bot,
    groupBot: GroupBot,
    group: GroupExtended,
    myMember: MemberAndPerson?,
    onOpenBot: (Bot) -> Unit,
    onBotRemoved: (Bot) -> Unit,
    onEditBot: (Bot) -> Unit
) {
    dialog(
        bot.name,
        cancelButton = null,
        confirmButton = application.appString { close },
        actions = {
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }
            var active by remember { mutableStateOf(groupBot.active == true) }

            menuTarget?.let {
                Menu({ menuTarget = null }, it) {
                    if (myMember?.member?.host == true) {
                        // todo: translate
                        item("Edit") {
                            onEditBot(bot)
                        }
                        // todo: translate
                        item("Remove") {
                            scope.launch {
                                val result = dialog(
                                    // todo: translate
                                    "Remove \"${bot.name}\" from this group?",
                                    // todo: translate
                                    confirmButton = "Yes, remove"
                                )

                                if (result == true) {
                                    api.deleteGroupBot(groupBot = groupBot.id!!) {
                                        onBotRemoved(bot)
                                    }
                                }
                            }
                        }
                    }
                    // todo: translate
                    item("Go to bot") {
                        onOpenBot(bot)
                    }
                }
            }

            // todo: translate
            IconButton("more_vert", "Menu") {
                menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
            }

            Switch(
                value = active,
                onValue = {},
                // todo: translate
                title = if (active) "Bot is running" else "Bot is paused",
                onChange = {
                    scope.launch {
                        api.updateGroupBot(
                            groupBot = groupBot.id!!,
                            update = GroupBot(active = it)
                        ) {
                            active = it.active == true
                        }
                    }
                },
                border = true
            )
        }
    ) {
        bot.description?.notBlank?.let {
            Text(it)
        }
    }
}
