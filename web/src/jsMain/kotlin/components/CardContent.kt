package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.softwork.routingcompose.Router
import appString
import com.queatz.db.Card
import com.queatz.db.CardOptions
import com.queatz.db.ConversationItem
import hint
import kotlinx.browser.window
import notBlank
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import stories.asStoryContents

@Composable
fun CardContent(card: Card) {
    val router = Router.current
    var cardConversation by remember { mutableStateOf<ConversationItem?>(null) }
    var isReplying by remember { mutableStateOf<List<ConversationItem>?>(null) }
    var replyMessage by remember { mutableStateOf("") }
    val stack = remember { mutableListOf<ConversationItem>() }
    val cardOptions = remember(card) { card.getOptions() }

    LaunchedEffect(card) {
        isReplying = null
        replyMessage = ""
        cardConversation = card.getConversation()
        stack.clear()
    }

    CardPhotoOrVideo(card)

    Div({
        classes(Styles.cardContent)
    }) {
        Div {
            Div {
                NameAndLocation(card.name, card.hint)
                val viewProfileString = appString { viewProfile }
                Span({
                    classes("material-symbols-outlined")
                    title(viewProfileString)
                    style {
                        cursor("pointer")
                        opacity(.5f)
                        marginLeft(.25.r)
                        property("vertical-align", "text-bottom")
                    }
                    onClick { event ->
                        if (event.ctrlKey) {
                            window.open("/profile/${card.person}", target = "_blank")
                        } else {
                            router.navigate("/profile/${card.person}")
                        }
                    }
                }) {
                    Text("person")
                }
            }
            card.categories?.firstOrNull()?.let { category ->
                Div({
                    classes(Styles.category)
                    style {
                        property("clear", "both")
                    }
                }) {
                    Text(category)
                }
            }
        }
        cardConversation?.message?.notBlank?.let { message ->
            Div({
                style {
                    whiteSpace("pre-wrap")
                }
            }) {
                LinkifyText(message)
            }
        }
        CardReply(
            card,
            cardOptions,
            cardConversation,
            stack,
            replyMessage,
            isReplying,
            onCardConversation = {
                cardConversation = it
            },
            onMessageSent = {
                replyMessage = ""
                isReplying = null
            },
            onIsReplying = {
                isReplying = it
            },
            onReplyMessage = {
                replyMessage = it
            },
            isLastElement = card.content?.asStoryContents()?.isNotEmpty() != true
        )
        Content(card.content)
    }
}
