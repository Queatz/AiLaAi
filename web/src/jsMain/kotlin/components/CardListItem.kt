package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.cardPeople
import app.dialog.photoDialog
import appString
import baseUrl
import bulletedString
import com.queatz.db.Card
import com.queatz.db.Person
import ellipsize
import focusable
import hint
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import people
import r

@Composable
fun CardListItem(
    card: Card,
    showPhoto: Boolean = true,
    showToolbar: Boolean = false,
    description: String? = null,
    people: List<Person>? = null,
    isOnSurface: Boolean = false,
    onBackground: Boolean = false,
    includeCardPerson: Boolean = false,
    largeFont: Boolean = false,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
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
            gap(1.r)
        }
    }) {
        if (showPhoto && allPhotos.isNotEmpty()) {
            PhotoPager(allPhotos, onPhotoClick = { url ->
                val initialIndex = allPhotos.indexOf(url.removePrefix(baseUrl)).coerceAtLeast(0)
                scope.launch {
                    photoDialog(
                        photos = allPhotos,
                        initialIndex = initialIndex
                    )
                }
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
            if (!showPhoto) {
                val collaborators = if (!includeCardPerson) card.collaborators.orEmpty() else card.people()

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
                        if (largeFont) {
                            fontSize(22.px)
                        }
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
            CardActivity(activity = it, card = card)
        }
        if (showToolbar) {
            CardToolbar(card)
        }
        card.getConversation().message.notBlank?.let { message ->
            Div {
                Text(message.stripMarkdown().ellipsize(120))
            }
        }
        if (showPhoto) {
            Button(
                attrs = {
                    classes(Styles.button)

                    style {
                        width(100.percent)
                    }

                    onClick {
                        onClick()
                    }
                }
            ) {
                Text(appString { this.viewDetails })
            }
        }
    }
}
