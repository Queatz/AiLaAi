package app.bots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.AppStyles
import app.dialog.dialog
import appString
import application
import com.queatz.db.Bot
import com.queatz.db.BotConfigField
import components.IconButton
import components.ProfilePhoto
import focusable
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun addBotDialog(
    onCreateBot: () -> Unit,
    onAddBot: (Bot) -> Unit,
) {
    dialog(
        title = application.appString { addBot },
        confirmButton = application.appString { close },
        cancelButton = null,
        actions = {
            IconButton("add", appString { createBot }) {
                onCreateBot()
            }
        }
    ) { resolve ->
        val bots by remember {
            mutableStateOf(
                listOf(
                    Bot(
                        name = "Bot 1",
                        description = "I am a bot"
                    ),
                    Bot(
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
                    )
                )
            )
        }

        bots.forEach { bot ->
            Div({
                classes(AppStyles.groupItem)

                focusable()

                onClick {
                    onAddBot(bot)
                    resolve(true)
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
