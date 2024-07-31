package app.bots

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.bots
import app.ailaai.api.groupBots
import app.components.Empty
import app.dialog.dialog
import appString
import application
import com.queatz.db.Bot
import components.IconButton
import components.LoadingText
import components.ProfilePhoto
import focusable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun addBotDialog(
    reload: SharedFlow<Unit>,
    group: String,
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
        var isLoading by remember {
            mutableStateOf(true)
        }
        var bots by remember {
            mutableStateOf(emptyList<Bot>())
        }

        suspend fun load() {
            api.groupBots(group) { groupBots ->
                api.bots {
                    bots = it.filter { bot ->
                        groupBots.none { it.bot!!.id!! == bot.id!! }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            load()
            isLoading = false
            reload.collectLatest {
                load()
            }
        }

        LoadingText(
            done = !isLoading,
            text = appString { loading }
        ) {
            if (bots.isEmpty()) {
                Empty {
                    // todo: translate
                    Text("No bots.")
                }
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
                    // todo: translate
                    ProfilePhoto(bot.photo, bot.name ?: "New bot")
                    Div({
                        style {
                            marginLeft(1.r)
                            property("max-width", "calc(100% - 2rem)")
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
                            Text(bot.description ?: "No description")
                        }
                    }
                }
            }
        }
    }
}
