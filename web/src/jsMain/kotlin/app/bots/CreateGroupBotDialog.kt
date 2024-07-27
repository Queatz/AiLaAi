package app.bots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.components.TextBox
import app.dialog.dialog
import com.queatz.db.Bot
import notBlank
import notEmpty
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun createGroupBotDialog(bot: Bot) {
    val result = dialog(
        title = bot.name!!,
        // todo: translate
        confirmButton = "Add to group",
    ) {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                gap(1.r)
            }
        }) {
            bot.description?.notBlank?.let {
                Text(it)
            }

            bot.config?.notEmpty?.let { config ->
                config.forEach {
                    var value by remember {
                        mutableStateOf("")
                    }
                    it.label?.notBlank?.let {
                        Div({
                            style {
                                fontSize(120.percent)
                            }
                        }) {
                            Text(it)
                        }
                    }
                    TextBox(
                        value = value,
                        onValue = { value = it },
                        placeholder = it.placeholder ?: "",
                        styles = {
                            margin(0.r)
                            width(32.r)
                            maxWidth(100.percent)
                        }
                    )
                }
            }
        }
    }
}
