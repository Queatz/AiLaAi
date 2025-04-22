package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.AppNavigation
import app.appNav
import app.softwork.routingcompose.Router
import appString
import application
import com.queatz.db.Card
import com.queatz.db.ConversationItem
import hint
import kotlinx.browser.window
import kotlinx.coroutines.launch
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
fun CardContent(
    card: Card,
    onCardClick: ((cardId: String, openInNewWindow: Boolean) -> Unit)? = null,
) {
    val me = application.me.collectAsState().value
    val scope = rememberCoroutineScope()
    val router = Router.current
    var cardConversation by remember { mutableStateOf<ConversationItem?>(null) }
    var isReplying by remember { mutableStateOf<List<ConversationItem>?>(null) }
    var replyMessage by remember { mutableStateOf("") }
    var replyMessageContact by remember { mutableStateOf("") }
    val stack = remember { mutableListOf<ConversationItem>() }
    val cardOptions = remember(card) { card.getOptions() }

    LaunchedEffect(card) {
        isReplying = null
        replyMessage = ""
        replyMessageContact = ""
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
                Icon(
                    name = "person",
                    title = viewProfileString,
                    onClick = { ctrlKey ->
                        if (ctrlKey) {
                            window.open("/profile/${card.person}", target = "_blank")
                        } else {
                            router.navigate("/profile/${card.person}")
                        }
                    },
                    styles = {
                        cursor("pointer")
                        opacity(.5f)
                        marginLeft(.25.r)
                        property("vertical-align", "text-bottom")
                    }
                )
                if (card.person == me?.id) {
                    Icon(
                        name = "edit",
                        title = appString { edit },
                        onClick = {
                            scope.launch {
                                appNav.navigate(AppNavigation.Page(id = card.id!!, card = card))
                            }
                        },
                        styles = {
                            cursor("pointer")
                            opacity(.5f)
                            marginLeft(.25.r)
                            property("vertical-align", "text-bottom")
                        }
                    )
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
            card = card,
            cardOptions = cardOptions,
            cardConversation = cardConversation,
            stack = stack,
            replyMessage = replyMessage,
            replyMessageContact = replyMessageContact,
            isReplying = isReplying,
            onCardConversation = {
                cardConversation = it
            },
            onMessageSent = {
                replyMessage = ""
                replyMessageContact = ""
                isReplying = null
            },
            onIsReplying = {
                isReplying = it
            },
            onReplyMessage = {
                replyMessage = it
            },
            onReplyMessageContact = {
                replyMessageContact = it
            },
            isLastElement = card.content?.asStoryContents()?.isNotEmpty() != true
        )
        Content(
            content = card.content,
            onCardClick = onCardClick,
        )
    }
}
