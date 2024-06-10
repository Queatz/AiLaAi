package components

import LocalConfiguration
import Strings.save
import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.cards
import app.nav.StoryNav
import appString
import appText
import com.queatz.ailaai.api.stories
import com.queatz.db.Card
import com.queatz.db.Geo
import com.queatz.db.StoryContent
import com.queatz.db.asGeo
import defaultGeo
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r
import stories.StoryContents
import stories.full

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
                        width(10.vw)
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
                    // todo translate
                    Text("or get it on")
                }
                A("https://play.google.com/store/apps/details?id=com.ailaai.app", {
                    target(ATarget.Blank)
                    style {
                        marginLeft(.5.r)
                        fontWeight("bold")
                        textDecoration("underline")
                    }
                }) {
                    Text("Google Play")
                }
                Span({
                    style {
                        opacity(.5f)
                    }
                }) {
                    // todo translate
                    Text(". iOS support is coming soon!")
                }
            }
        }

        SearchField(searchText, appString { searchCity }, styles = {
            marginTop(2.r)
        }) {
            searchText = it
        }

        val hcmc = Geo(10.7915858, 106.7426523)

        LaunchedEffect(searchText) {
            isLoading = true
            delay(250)

            if (searchText.isNotBlank()) {
                api.cards(hcmc, search = searchText, onError = {
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
                var storyContent by remember { mutableStateOf<List<StoryContent>>(emptyList()) }

                LaunchedEffect(Unit) {
                    api.stories(hcmc) { stories ->
                        storyContent = stories.flatMapIndexed { index, it ->
                            if (index < stories.lastIndex) it.full() + StoryContent.Divider else it.full()
                        }
                    }
                }

                Div({
                    classes(Styles.cardContent)
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(1.r)
                    }
                }) {
                    StoryContents(
                        storyContent,
                        onGroupClick = {
                            window.open("/groups/${it.group!!.id!!}", "_blank")
                        },
                        openInNewWindow = true
                    )
                }
            }

            false -> {
                listOf(
                    (if (isLoading) appString { searching } else appString { this.searchResults }) to searchResults,
                ).forEach { (category, cards) ->
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
                                CardItem(card, styles = {
                                    margin(1.r)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
