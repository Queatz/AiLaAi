
import androidx.compose.runtime.Composable
import app.cards.MapList
import app.components.Empty
import components.CardContent
import components.IconButton
import kotlinx.browser.window
import com.queatz.db.Card
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun MapCardPanel(
    selectedCard: Card?,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    shownCards: List<Card>,
    onCardClick: (Card) -> Unit,
    cardNavHistory: List<Card>,
    onBack: () -> Unit,
) {
    Div({
        classes(Styles.navContent)
    }) {
        if (selectedCard == null) {
            Div({
                classes(Styles.stickyHeader)
                style {
                    fontWeight("bold")
                    padding(1.r)
                    fontSize(24.px)
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.SpaceBetween)
                }
            }) {
                Text(appString { explore })
                IconButton(
                    name = if (expanded) "expand_less" else "expand_more",
                    title = if (expanded) appString { collapse } else appString { expand },
                    styles = {
                        borderRadius(2.r)
                    }
                ) {
                    onExpandChange(!expanded)
                }
            }
            if (expanded) {
                if (shownCards.isEmpty()) {
                    Empty { appText { noCardsNearby } }
                } else {
                    MapList(
                        cards = shownCards,
                        styles = {
                            padding(1.r)
                        }
                    ) { card ->
                        onCardClick(card)
                    }
                }
            }
        } else {
            Div({
                classes(Styles.stickyHeader)
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.SpaceBetween)
                    padding(1.r)
                }
            }) {
                IconButton(
                    name = "arrow_back",
                    title = appString { goBack },
                    background = true
                ) {
                    onBack()
                }
                IconButton(
                    name = "open_in_new",
                    title = appString { openPage },
                    background = true
                ) {
                    window.open("/page/${selectedCard!!.id!!}", target = "_blank")
                }
            }
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    justifyContent(JustifyContent.Center)
                    property("flex", "1")
                    paddingLeft(1.r)
                    paddingRight(1.r)
                }
            }) {
                Span({
                    style {
                        fontWeight("bold")
                    }
                }) {
                    Text(selectedCard?.name ?: appString { newCard })
                }
                selectedCard?.hint?.notBlank?.let { hint ->
                    Span({
                        style {
                            fontSize(14.px)
                            opacity(.75f)
                        }
                    }) {
                        Text(hint)
                    }
                }
            }
            CardContent(
                card = selectedCard!!,
                showTitle = false
            )
        }
    }
}
