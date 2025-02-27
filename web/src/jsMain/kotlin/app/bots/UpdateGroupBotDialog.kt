package app.bots

import api
import app.ailaai.api.updateGroupBot
import app.dialog.dialog
import application
import com.queatz.db.Bot
import com.queatz.db.GroupBot
import kotlinx.coroutines.flow.MutableStateFlow
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun updateGroupBotDialog(
    bot: Bot,
    groupBot: GroupBot,
    onUpdated: (GroupBot) -> Unit
) {
    val values = MutableStateFlow(groupBot.config ?: emptyList())

    val result = dialog(
        title = bot.name!!,
        confirmButton = application.appString { update },
    ) {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
                maxWidth(32.r)
            }
        }) {
            bot.description?.notBlank?.let {
                Text(it)
            }

            bot.config?.let {
                BotConfigValues(it, groupBot.config) {
                    values.value = it
                }
            }
        }
    }

    if (result == true) {
        api.updateGroupBot(
            groupBot = groupBot.id!!,
            update = GroupBot(config = values.value)
        ) {
            onUpdated(it)
        }
    }
}
