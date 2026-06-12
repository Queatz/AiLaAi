package components

import androidx.compose.runtime.*
import api
import app.AppStyles
import app.ailaai.api.cardPeople
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
import ellipsize
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.marginTop
import r

@Composable
fun CardListItem(
    card: Card,
    showPhoto: Boolean = true,
    description: String? = null,
    people: List<Person>? = null,
    isOnSurface: Boolean = false,
    onBackground: Boolean = false,
    onClick: () -> Unit,
) {
    var loadedPeople by remember { mutableStateOf(emptyList<Person>()) }
    val allPhotos = remember(card.photo, card.photos) {
        listOfNotNull(card.photo) + (card.photos ?: emptyList())
    }

    LaunchedEffect(card.id, people) {
        val id = card.id ?: return@LaunchedEffect
        if (card.collaborators.orEmpty().any { cid -> people?.none { it.id == cid } != false }) {
            api.cardPeople(id) {
                loadedPeople = it
            }
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(.5.r)
        }
    }) {
        if (showPhoto && allPhotos.isNotEmpty()) {
            PhotoPager(allPhotos, onPhotoClick = {
                onClick()
            }, attrs = {
                classes(AppStyles.groupItemMainPhoto)
            })
        }
        Div({
            classes(AppStyles.groupItem)

            if (isOnSurface) {
                classes(AppStyles.groupItemOnSurface)
            }

            focusable()

            style {
                padding(.5.r)
            }

            onClick {
                onClick()
            }
        }) {
            val collaborators = card.collaborators.orEmpty()

            if (!showPhoto) {
                if (collaborators.isNotEmpty()) {
                    val items = buildList {
                        collaborators.forEach { id ->
                            val p = people?.find { it.id == id } ?: loadedPeople.find { it.id == id }
                            add(GroupPhotoItem(p?.photo, p?.name))
                        }
                    }
                    GroupPhoto(items = items, size = 54.px)
                } else {
                    ProfilePhoto(
                        photo = card.photo,
                        name = card.name ?: appString { newCard },
                        size = 54.px,
                        onBackground = onBackground,
                    )
                }
            }

            Div({
                style {
                    if (!showPhoto) {
                        marginLeft(1.r)
                    }
                    property("max-width", "calc(100% - 4rem)")
                }
            }) {
                Div({
                    classes(AppStyles.groupItemName)

                    style {
                        fontWeight("bold")
                        fontSize(22.px)
                    }
                }) {
                    Text(card.name ?: appString { newCard })
                }
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(
                        bulletedString(
                            description,
                            card.hint.notBlank
                        )
                    )
                }
            }
        }
        card.activity?.let {
            CardActivity(it)
        }
        if (showPhoto) {
            CardToolbar(card) {
                marginTop(.5.r)
            }
        }
        card.getConversation().message.notBlank?.let { message ->
            Div {
                Text(message.stripMarkdown().ellipsize(120))
            }
        }
    }
}
