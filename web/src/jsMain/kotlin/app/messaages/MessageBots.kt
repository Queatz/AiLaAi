package app.messaages

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.AppStyles
import com.queatz.db.Bot
import com.queatz.db.BotMessageStatus
import components.GroupPhoto
import components.GroupPhotoItem
import components.Icon
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun MessageBots(
    bots: List<Bot>,
    statuses: List<BotMessageStatus>,
    isMine: Boolean
) {
    Div({
        classes(listOf(AppStyles.messageBots) + (if (isMine) listOf(AppStyles.myMessageBots) else emptyList()))
    }) {
        GroupPhoto(
            items = remember(bots, statuses) {
                statuses.mapNotNull { status ->
                    bots.find { it.id == status.bot }?.let { bot ->
                        GroupPhotoItem(bot.photo, "${bot.name}: ${if (status.success == true) "✅" else "❌"} ${status.note.orEmpty()}")
                    }
                }
            },
            size = 24.px,
            mergeTitles = true,
            fallback = "smart_toy",
            fallbackTitle = remember(bots, statuses) {
                statuses.joinToString("\n") { status ->
                    bots.find { it.id == status.bot }.let { bot ->
                        "${bot?.name ?: "\uD83E\uDD16"}: ${if (status.success == true) "✅" else "❌"} ${status.note.orEmpty()}"
                    }
                }
            }
        )
        val success = statuses.all { it.success == true }
        Icon(if (success) "check" else "clear") {
            property("pointer-events", 
                "none")
            position(Position.Absolute)
            bottom(-2.px)
            right(2.px)
            borderRadius(1.r)
            backgroundColor(if (success) Styles.colors.green else Styles.colors.red)
            color(Color.white)
            fontSize(12.px)
        }
    }
}
