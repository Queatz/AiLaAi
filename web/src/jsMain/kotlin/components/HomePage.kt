package components

import LocalConfiguration
import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.cards
import appString
import appText
import com.queatz.db.Card
import com.queatz.db.Geo
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r

@Composable
fun HomePage() {
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(listOf<Card>()) }

    Div({
        style {
            property("margin", "${1.r} auto")
            maxWidth(1200.px)
            padding(0.r, 1.r, 1.r, 1.r)
            fontSize(22.px)
            lineHeight("1.5")
            minHeight(100.vh)
        }
    }) {
        Div({
            classes(Styles.mainHeader)
        }) {
            Div {
                Text(appString { homeTagline })
            }
        }
        H3 {
            Text(appString { homeAboutTitle })
        }
        Div {
            Text(appString { homeAboutDescription })
            Br()
        }

        val lang = LocalConfiguration.current.language

        Div({
            style {
                display(DisplayStyle.Flex)
                marginTop(2.r)
                overflowX("auto")
            }
        }) {
            repeat(6) {
                Img(src = "/screenshots/$lang/000${it + 1}.png") {
                    style {
                        borderRadius(1.r)
                        marginRight(1.r)
                        width(20.vw)
                    }
                }
            }
        }

        Div({
            classes(Styles.mobileRow)
            style {
                alignItems(AlignItems.Center)
                marginTop(2.r)
            }
        }) {
            DownloadAppButton()
            Div {
                Span({
                    style {
                        marginLeft(1.r)
                        opacity(.5f)
                    }
                }) {
                    Text("or get it on")
                }
                A("https://play.google.com/store/apps/details?id=com.ailaai.app", {
                    target(ATarget.Blank)
                    style {
                        marginLeft(.5.r)
                        fontWeight("bold")
                    }
                }) {
                    Text("Google Play")
                }
            }
        }

        SearchField(searchText, appString { searchCity }, styles = {
            marginTop(2.r)
        }) {
            searchText = it
        }

        LaunchedEffect(searchText) {
            isLoading = true
            delay(250)


            if (searchText.isNotBlank()) {
                // HCMC
                api.cards(Geo(10.7915858, 106.7426523), search = searchText, onError = {
                    searchResults = emptyList()
                }) {
                    searchResults = it
                }
            } else {
                searchResults = emptyList()
            }
            isLoading = false
        }

        when (searchText.isBlank()) {
            true -> {
                listOf(
                    appString { peopleToKnow } to listOf(
                        "11389583",
                        "11156377",
                        "10455696",
                        "12319827",
                        "9914441"
                    ).shuffled().take(3),
                    appString { placesToKnow } to listOf("9879608", "10102613")
                )
            }

            false -> {
                listOf(
                    (if (isLoading) appString { searching } else appString { this.searchResults }) to searchResults,
                )
            }
        }.forEach { (category, cards) ->
            H3 {
                Text(category)
            }
            Div({
                classes(Styles.mainContentCards)
            }) {
                if (cards.isEmpty()) {
                    if (!isLoading) {
                        Span({
                            style {
                                color(Styles.colors.secondary)
                            }
                        }) {
                            appText { noCards }
                        }
                    }
                } else {
                    cards.forEach { card ->
                        when (card) {
                            is String -> CardItem(card) {
                                margin(1.r)
                            }

                            is Card -> CardItem(card, styles = {
                                margin(1.r)
                            })
                        }
                    }
                }
            }
        }
    }
}
