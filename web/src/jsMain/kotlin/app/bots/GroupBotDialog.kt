package app.bots

import app.dialog.dialog
import com.queatz.db.Bot
import notBlank
import org.jetbrains.compose.web.dom.Text

suspend fun groupBotDialog(bot: Bot) {
    dialog(
        bot.name
    ) {
        bot.description?.notBlank?.let {
            Text(it)
        }
    }
}
