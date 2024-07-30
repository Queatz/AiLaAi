package app.bots

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.groupBots
import app.components.Empty
import app.dialog.dialog
import app.menu.Menu
import appString
import application
import com.queatz.db.Bot
import com.queatz.db.GroupBot
import com.queatz.db.GroupBotExtended
import components.IconButton
import components.LoadingText
import components.ProfilePhoto
import focusable
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import r

suspend fun groupBotsDialog(
    group: String,
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
        var isLoading by remember {
            mutableStateOf(true)
        }
        var groupBots by remember {
            mutableStateOf(emptyList<GroupBotExtended>())
        }

        LaunchedEffect(Unit) {
            api.groupBots(group = group) {
                groupBots = it
            }
            isLoading = false
        }

        LoadingText(
            done = !isLoading,
            text = appString { loading }
        ) {
            if (groupBots.isEmpty()) {
                Empty {
                    // todo: translate
                    Text("No bots.")
                }
            } else {
                groupBots.forEach { groupBot ->
                    val bot = groupBot.bot!!

                    key(groupBot.groupBot!!.id!!) {
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
                                onBot(bot, groupBot.groupBot!!)
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
                                    Text(if (groupBot.groupBot!!.active == true) "Running" else "Paused")
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
}
