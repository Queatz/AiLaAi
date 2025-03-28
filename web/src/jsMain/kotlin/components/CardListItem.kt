package components

import androidx.compose.runtime.Composable
import app.AppStyles
import com.queatz.db.Card
import focusable
import hint
import notBlank
import baseUrl
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import r

@Composable
fun CardListItem(
    card: Card,
    showPhoto: Boolean = true,
    onClick: () -> Unit,
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        if (card.photo != null) {
            Div({
                style {
                    backgroundImage("url($baseUrl${card.photo!!})")
                    backgroundPosition("center")
                    backgroundSize("cover")
                    property("aspect-ratio", "1.5")
                    property("width", "100%")
                    borderRadius(1.r)
                }

                onClick {
                    onClick()
                }
            }) {}
        }
        Div({
            classes(AppStyles.groupItem)

            focusable()

            style {
                padding(.5.r)
            }

            onClick {
                onClick()
            }
        }) {
            // todo: translate
            ProfilePhoto(card.photo, card.name ?: "New page", size = 54.px)
            Div({
                style {
                    marginLeft(1.r)
                    property("max-width", "calc(100% - 4rem)")
                }
            }) {
                Div({
                    classes(AppStyles.groupItemName)

                    style {
                        fontWeight("bold")
                    }
                }) {
                    // todo: translate
                    Text(card.name ?: "New page")
                }
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(
                        listOfNotNull(
                            card.categories?.first(),
                            card.hint.notBlank,
                            card.getConversation().message.notBlank
                        ).joinToString(" • ")
                    )
                }
            }
        }
    }
}
