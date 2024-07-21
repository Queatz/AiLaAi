package app.cards

import MapView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.FullPageLayout
import app.ailaai.api.card
import app.ailaai.api.cards
import app.ailaai.api.savedCards
import app.components.TopBarSearch
import app.nav.CardNav
import appText
import application
import com.queatz.db.Card
import com.queatz.db.asGeo
import components.CardItem
import components.Loading
import defaultGeo
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun CardsPage(nav: CardNav, onCard: (CardNav) -> Unit, onCardUpdated: (Card) -> Unit) {
    Style(CardsPageStyles)

    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var cards by remember(nav) {
        mutableStateOf(listOf<Card>())
    }
    var isLoading by remember(nav !is CardNav.Selected) {
        mutableStateOf(nav !is CardNav.Selected)
    }

    var search by remember {
        mutableStateOf("")
    }

    LaunchedEffect(nav) {
        if (nav !is CardNav.Selected) {
            isLoading = true
        }
    }

    suspend fun reload() {
        if (me == null) return

        when (nav) {
            is CardNav.Map -> {
                api.cards(me?.geo?.asGeo() ?: defaultGeo, search = search.notBlank, public = true) {
                    cards = it
                }
            }

            is CardNav.Friends -> {
                api.cards(me?.geo?.asGeo() ?: defaultGeo, search = search.notBlank) {
                    cards = it
                }
            }

            is CardNav.Local -> {
                api.cards(me?.geo?.asGeo() ?: defaultGeo, public = true, search = search.notBlank) {
                    cards = it
                }
            }

            is CardNav.Saved -> {
                api.savedCards(search = search.notBlank) {
                    cards = it.mapNotNull { it.card }
                }
            }

            is CardNav.Selected -> {
                // Nothing to load
            }
        }
        isLoading = false
    }

    LaunchedEffect(nav, search) {
        reload()
    }

    if (isLoading) {
        Loading()
    } else {
        FullPageLayout(maxWidth = null) {
            if (nav !is CardNav.Selected) {
                if (nav is CardNav.Map) {
                    Div({
                        style {
                            flexGrow(1)
                            borderRadius(1.r)
                            margin(1.r)
                            position(Position.Relative)
                            overflow("hidden")
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                        }
                    }) {
                        MapView()
                    }
                } else {
                    if (cards.isEmpty()) {
                        Div({
                            style {
                                height(100.percent)
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.Center)
                                opacity(.5)
                            }
                        }) {
                            when (nav) {
                                is CardNav.Friends -> appText { noCards }
                                is CardNav.Local -> appText { noCardsNearby }
                                is CardNav.Saved -> appText { noSavedCards }
                                else -> {}
                            }
                        }
                    } else {
                        Div(
                            {
                                classes(CardsPageStyles.layout)

                                style {
                                    overflowX("hidden")
                                    overflowY("auto")
                                }
                            }
                        ) {
                            cards.forEach {
                                CardItem(it, openInNewWindow = true)
                            }
                        }
                    }
                }
            } else {
                ExplorePage(
                    card = nav.subCard ?: nav.card,
                    onCard = {
                        onCard(CardNav.Selected(it))
                    },
                    onCardUpdated = {
                        onCardUpdated(it)
                    },
                    onCardDeleted = {
                        if (it.parent != null) {
                            scope.launch {
                                api.card(it.parent!!) {
                                    onCard(CardNav.Selected(it))
                                    onCardUpdated(it)
                                }
                            }
                        } else {
                            onCard(CardNav.Local)
                            onCardUpdated(it)
                        }
                    }
                )
            }
        }
        if (nav !is CardNav.Selected && nav !is CardNav.Map) {
            TopBarSearch(search, { search = it })
        }
    }
}
