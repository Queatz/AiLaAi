package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.card
import app.ailaai.api.cardsCards
import app.softwork.routingcompose.Router
import appString
import application
import com.queatz.db.Card
import com.queatz.db.CardOptions
import com.queatz.db.ConversationItem
import hint
import json
import kotlinx.browser.window
import mainContent
import notBlank
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import webBaseUrl

@OptIn(ExperimentalComposeWebApi::class)
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
    val layout by application.layout.collectAsState()

    application.layout.collectAsState()

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

    if (layout == AppLayout.Kiosk) {
        QrImg("$webBaseUrl/page/$cardId") {
            position(Position.Fixed)
            bottom(1.r)
            left(1.r)
            transform {
                scale(2)
                translate(25.percent, -25.percent)
            }
        }
    }

    if (!isLoading && card == null) {
        Div({
            mainContent(layout)
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
            mainContent(layout)
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
                            Content(card.content)
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
