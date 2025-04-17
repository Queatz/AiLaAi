package app.bots

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.bot
import app.ailaai.api.deleteGroupBot
import app.ailaai.api.groupBot
import app.ailaai.api.updateGroupBot
import app.dialog.dialog
import app.menu.Menu
import appString
import application
import com.queatz.db.Bot
import com.queatz.db.GroupBot
import com.queatz.db.MemberAndPerson
import components.IconButton
import components.Switch
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement

suspend fun groupBotDialog(
    scope: CoroutineScope,
    reload: SharedFlow<Unit>,
    bot: Bot,
    groupBot: GroupBot,
    myMember: MemberAndPerson?,
    onOpenBot: (Bot) -> Unit,
    onGroupBotUpdated: (GroupBot) -> Unit,
    onBotRemoved: (Bot) -> Unit,
    onEditBot: (Bot, GroupBot) -> Unit
) {
    dialog(
        title = bot.name,
        cancelButton = null,
        confirmButton = application.appString { close },
        actions = { resolve ->
            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }
            var active by remember { mutableStateOf(groupBot.active == true) }

            var bot by remember { mutableStateOf(bot) }
            var groupBot by remember { mutableStateOf(groupBot) }

            LaunchedEffect(Unit) {
                reload.collectLatest {
                    api.bot(bot.id!!, onError = {
                        if ((it as? ResponseException)?.response?.status == HttpStatusCode.NotFound) {
                            resolve(null)
                        }
                    }) {
                        bot = it
                    }
                    api.groupBot(groupBot.id!!, onError = {
                        if ((it as? ResponseException)?.response?.status == HttpStatusCode.NotFound) {
                            resolve(null)
                        }
                    }) {
                        groupBot = it
                    }
                }
            }

            menuTarget?.let {
                Menu({ menuTarget = null }, it) {
                    if (myMember?.member?.host == true) {
                        item(appString { edit }) {
                            onEditBot(bot, groupBot)
                        }
                        item(appString { remove }) {
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
                                        resolve(null)
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
                            onGroupBotUpdated(it)
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
