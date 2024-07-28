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
import components.IconButton
import components.ProfilePhoto
import focusable
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r

suspend fun groupBotsDialog(
    onAddBot: () -> Unit,
    onBot: (Bot) -> Unit,
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
                        name = "Bot 1",
                        description = "I am a bot"
                    ).apply { id = "1" },
                    Bot(
                        name = "Bot 2",
                        description = "I am a bot, two"
                    ).apply { id = "2" }
                )
            )
        }

        if (bots.isEmpty()) {
            Empty {
                Text("No bots.")
            }
        } else {
            bots.forEach { bot ->
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
                            onBot(bot)
                        }
                    }) {
                        ProfilePhoto(bot.photo, bot.name ?: "New bot")
                        Div({
                            style {
                                marginLeft(1.r)
                            }
                        }) {
                            Div({
                                classes(AppStyles.groupItemName)
                            }) {
                                Text(bot.name ?: "New bot")
                            }
                            Div({
                                classes(AppStyles.groupItemMessage)
                            }) {
                                Text(bot.description ?: "No description")
                            }
                        }
                    }
                }
            }
        }
    }
}
