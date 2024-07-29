package app.bots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import app.components.Empty
import app.dialog.dialog
import app.menu.Menu
import appString
import application
import com.queatz.db.Bot
import com.queatz.db.BotConfigField
import com.queatz.db.GroupBot
import components.IconButton
import components.ProfilePhoto
import focusable
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import r

suspend fun groupBotsDialog(
    onAddBot: () -> Unit,
    onBot: (Bot, GroupBot) -> Unit,
) {
    dialog(
        title = application.appString { groupBots },
        confirmButton = application.appString { close },
        cancelButton = null,
        actions = {
            IconButton("add", appString { addBot }) {
                onAddBot()
            }
        }
    ) { resolve ->
        val bots by remember {
            mutableStateOf(
                listOf(
                    Bot(
                        url = "https://bot.app",
                        name = "Bot 1",
                        description = "I am a bot"
                    ).apply { id = "1" } to GroupBot(active = false),
                    Bot(
                        url = "https://bot.app",
                        name = "Bot 2",
                        description = "I am a bot, two",
                        config = listOf(
                            BotConfigField(
                                key = "key",
                                label = "Your name",
                                placeholder = "First last",
                                required = true
                            )
                        )
                    ).apply { id = "2" } to GroupBot(active = true)
                )
            )
        }

        if (bots.isEmpty()) {
            Empty {
                Text("No bots.")
            }
        } else {
            bots.forEach { (bot, groupBot) ->
                key(bot.id!!) {
                    var menuTarget by remember {
                        mutableStateOf<DOMRect?>(null)
                    }

                    if (menuTarget != null) {
                        Menu({ menuTarget = null }, menuTarget!!) {
                            item(appString { delete }) {
                                //
                            }
                            if (application.me.value?.id == bot.creator) {
                                item(appString { openBot }) {
                                    //
                                }
                            }
                        }
                    }

                    Div({
                        classes(AppStyles.groupItem)

                        focusable()

                        onClick {
                            onBot(bot, groupBot)
                        }
                    }) {
                        // todo: translate
                        ProfilePhoto(bot.photo, bot.name ?: "New bot")
                        Div({
                            style {
                                marginLeft(1.r)
                            }
                        }) {
                            Div({
                                classes(AppStyles.groupItemName)
                            }) {
                                // todo: translate
                                Text(bot.name ?: "New bot")
                            }
                            Div({
                                classes(AppStyles.groupItemMessage)
                            }) {
                                // todo: translate
                                Text(if (groupBot.active == true) "Running" else "Paused")
                                Text(" • ")
                                // todo: translate
                                Text(bot.description ?: "No description")
                            }
                        }
                    }
                }
            }
        }
    }
}
