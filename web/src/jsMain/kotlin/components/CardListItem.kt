package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.AppStyles
import appString
import com.queatz.db.Card
import com.queatz.db.Person
import components.GroupPhoto
import components.GroupPhotoItem
import focusable
import hint
import notBlank
import baseUrl
import bulletedString
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
    people: List<Person>? = null,
    onClick: () -> Unit,
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        if (showPhoto && card.photo != null) {
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
            })
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
            val collaborators = card.collaborators.orEmpty()

            if (collaborators.isNotEmpty()) {
                val items = buildList {
                    val ownerPerson = people?.find { it.id == card.person }
                    add(GroupPhotoItem(ownerPerson?.photo ?: card.photo, ownerPerson?.name ?: card.name))

                    collaborators.forEach { id ->
                        val p = people?.find { it.id == id }
                        add(GroupPhotoItem(p?.photo, p?.name))
                    }
                }
                GroupPhoto(items = items, size = 54.px)
            } else {
                ProfilePhoto(card.photo, card.name ?: appString { newCard }, size = 54.px)
            }

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
                    Text(card.name ?: appString { newCard })
                }
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(
                        bulletedString(
                            card.hint.notBlank,
                            card.getConversation().message.notBlank
                        )
                    )
                }
            }
        }
    }
}
