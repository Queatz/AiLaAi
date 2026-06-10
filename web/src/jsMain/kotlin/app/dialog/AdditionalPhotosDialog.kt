package app.dialog

import androidx.compose.runtime.*
import api
import app.ailaai.api.updateCard
import application
import baseUrl
import com.queatz.db.Card
import components.IconButton
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import r

suspend fun additionalPhotosDialog(
    card: Card,
    onCardUpdated: (Card) -> Unit
) {
    dialog(
        title = application.appString { additionalPhotos },
        confirmButton = application.appString { done },
        cancelButton = null
    ) { resolve ->
        var photos by remember { mutableStateOf(card.photos ?: emptyList()) }
        val choosePhotoDialog = rememberChoosePhotoDialog(showUpload = true)

        Div {
            photos.forEachIndexed { index, photo ->
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(1.r)
                        marginBottom(1.r)
                    }
                }) {
                    Img("$baseUrl$photo") {
                        style {
                            width(4.r)
                            height(4.r)
                            property("object-fit", "cover")
                            borderRadius(1.r)
                        }
                    }

                    Div({ style { flex(1) } })

                    if (index > 0) {
                        IconButton("arrow_upward", application.appString { moveUp }) {
                            photos = photos.toMutableList().apply {
                                val item = removeAt(index)
                                add(index - 1, item)
                            }
                        }
                    }
                    if (index < photos.size - 1) {
                        IconButton("arrow_downward", application.appString { moveDown }) {
                            photos = photos.toMutableList().apply {
                                val item = removeAt(index)
                                add(index + 1, item)
                            }
                        }
                    }

                    IconButton("delete", application.appString { remove }) {
                        photos = photos.toMutableList().apply { removeAt(index) }
                    }
                }
            }

            IconButton("add", application.appString { addPhoto }, text = application.appString { addPhoto }) {
                choosePhotoDialog.launch(multiple = true) { photo, _, _ ->
                    photos = photos + photo
                }
            }
        }

        LaunchedEffect(photos) {
            if (photos != card.photos) {
                api.updateCard(card.id!!, Card(photos = photos)) {
                    onCardUpdated(it)
                }
            }
        }
    }
}
