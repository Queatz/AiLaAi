package app.nav

import Styles
import androidx.compose.runtime.*
import app.AppStyles
import appString
import com.queatz.db.Card
import components.Icon
import focusable
import hint
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import r

@Composable
fun CardItem(
    card: Card,
    scroll: Boolean,
    selected: Boolean,
    saved: Boolean,
    published: Boolean,
    onSelected: (Boolean) -> Unit
) {
    var ref by remember {
        mutableStateOf<HTMLElement?>(null)
    }

    if (scroll) {
        LaunchedEffect(card, selected, ref) {
            if (selected) {
                ref?.let { ref ->
                    val rect = ref.getBoundingClientRect()
                    val parentRect = ref.offsetParent!!.getBoundingClientRect()
                    if (rect.y > parentRect.y + parentRect.height || rect.y + rect.height < parentRect.y) {
                        ref.scrollIntoView()
                    }
                }
            }
        }
    }

    Div({
        classes(
            listOf(AppStyles.groupItem) + if (selected) {
                listOf(AppStyles.groupItemSelected)
            } else {
                emptyList()
            }
        )
        focusable()

        onClick {
            onSelected(false)
        }

        onDoubleClick {
            onSelected(true)
        }

        if (scroll) {
            ref {
                ref = it
                onDispose { }
            }
        }
    }) {
        Div({
            style {
                width(0.px)
                flexGrow(1)
            }
        }) {
            Div({
                classes(AppStyles.groupItemName)
            }) {
                Text(card.name?.notBlank ?: appString { newCard })
            }
            if (card.hint.isNotBlank()) {
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(card.hint)
                }
            }
        }
        Div({
            style {
                marginLeft(.5.r)
                flexShrink(0)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
            }
        }) {
            if ((card.cardCount ?: 0) > 0) {
                Span({
                    style {
                        color(Styles.colors.secondary)
                        fontSize(14.px)
                        opacity(.5)
                    }
                }) {
                    Text(
                        "${card.cardCount} ${appString { if (card.cardCount == 1) inlineCard else inlineCards }}"
                    )
                }
            }
            if (published) {
                Icon("toggle_on", title = "Page is published") { // todo: translate
                    fontSize(22.px)
                    color(Styles.colors.primary)
                    marginLeft(.5.r)
                }
            }
            if (saved) {
                Icon("favorite", title = "Page is saved") { // todo: translate
                    fontSize(18.px)
                    color(Styles.colors.secondary)
                    marginLeft(.5.r)
                }
            }
        }
    }
}
