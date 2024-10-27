package components

import AppLayout
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
import app.ailaai.api.cardByUrl
import app.ailaai.api.cardsCards
import app.softwork.routingcompose.Router
import appString
import application
import baseUrl
import com.queatz.db.Card
import com.queatz.db.CardOptions
import com.queatz.db.ConversationItem
import json
import mainContent
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import webBaseUrl

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun CardPage(
    cardId: String? = null,
    url: String? = null,
    onError: () -> Unit = {},
    cardLoaded: (card: Card) -> Unit,
) {
    var cardId by remember { mutableStateOf(cardId) }
    var isLoading by remember { mutableStateOf(false) }
    var card by remember { mutableStateOf<Card?>(null) }
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    val layout by application.layout.collectAsState()
    val router = Router.current

    application.layout.collectAsState()

    application.background(card?.background?.let { "$baseUrl$it" })

    LaunchedEffect(cardId, url) {
        if (card != null && card?.id == cardId) {
            return@LaunchedEffect
        }
        isLoading = true
        card = null
        cards = emptyList()
        try {
            if (cardId != null) {
                api.card(cardId!!) {
                    card = it
                }
            } else if (url != null) {
                api.cardByUrl(url) {
                    cardId = it.id!!
                    card = it
                }
            } else {
                router.navigate("/")
                return@LaunchedEffect
            }
            cardLoaded(card!!)
            api.cardsCards(card!!.id!!) {
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
            bottom(2.r)
            left(2.r)
            maxWidth(10.vw)
            maxHeight(10.vw)
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
                    card?.let {
                        CardContent(it)
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
