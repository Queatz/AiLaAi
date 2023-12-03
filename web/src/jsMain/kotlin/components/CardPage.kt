package components

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.softwork.routingcompose.Router
import appString
import com.queatz.db.Card
import com.queatz.db.CardOptions
import com.queatz.db.ConversationItem
import hint
import json
import kotlinx.browser.window
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun CardPage(cardId: String, onError: () -> Unit = {}, cardLoaded: (card: Card) -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    var card by remember { mutableStateOf<Card?>(null) }
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    val stack = remember { mutableListOf<ConversationItem>() }
    var cardConversation by remember { mutableStateOf<ConversationItem?>(null) }
    var cardOptions by remember { mutableStateOf<CardOptions?>(null) }
    var isReplying by remember { mutableStateOf<List<ConversationItem>?>(null) }
    var replyMessage by remember { mutableStateOf("") }
    val router = Router.current

    LaunchedEffect(cardId) {
        isReplying = null
        replyMessage = ""
        isLoading = true
        card = null
        cards = emptyList()
        try {
            api.card(cardId) {
                card = it
            }
            cardConversation = card!!.getConversation()
            cardOptions = card!!.getOptions()
            stack.clear()
            cardLoaded(card!!)
            api.cardsCards(cardId) {
                cards = it
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            onError()
        } finally {
            isLoading = false
        }
    }

    if (!isLoading && card == null) {
        Div({
            classes(Styles.mainContent)
            style {
                display(DisplayStyle.Flex)
                minHeight(100.vh)
                width(100.percent)
                flexDirection(FlexDirection.Column)
                padding(2.r)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.FlexStart)
            }
        }) {
            Text(appString { cardNotFound })
        }
    } else {
        Div({
            classes(Styles.mainContent)
        }) {
            Div({
                classes(Styles.navContainer)
            }) {
                Div({
                    classes(Styles.navContent)
                }) {
                    card?.let { card ->
                        CardPhotoOrVideo(card)
                    }
                    Div({
                        classes(Styles.cardContent)
                    }) {
                        card?.let { card ->
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
                                }
                            )
                            CardContent(card)
                        }
                    }
                }
            }
            Div({
                classes(Styles.content)
            }) {
                cards.forEach { card ->
                    CardItem(card, styles = {
                        margin(1.r)
                    })
                }
            }
        }
    }
}

fun Card.getConversation() = json.decodeFromString<ConversationItem>(conversation ?: "{}")

fun Card.getOptions() = json.decodeFromString<CardOptions>(options ?: "{}")
