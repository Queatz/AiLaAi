package app

import androidx.compose.runtime.*
import api
import app.ailaai.api.stickerPacks
import com.queatz.db.Sticker
import com.queatz.db.StickerPack
import components.Loading
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

val allStickerPacks = MutableStateFlow<List<StickerPack>>(emptyList())

@Composable
fun StickersTray(onSticker: (Sticker) -> Unit) {
    val stickerPacks by allStickerPacks.collectAsState()

    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        api.stickerPacks {
            allStickerPacks.value = it
        }
        isLoading = false
    }

    if (isLoading) {
        Loading()
    } else if (stickerPacks.isNotEmpty()) {
        Div({
            style {
                padding(1.r)
            }
        }) {
            stickerPacks.forEach { stickerPack ->
                Div({
                    style {
                        fontWeight("bold")
                        marginBottom(.5.r)
                    }
                }) {
                    Text(stickerPack.name ?: "Stickers")
                }
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        marginBottom(1.r)
                        overflowX("auto")
                        property("scrollbar-width", "none")
                    }
                }) {
                    stickerPack.stickers?.forEach { sticker ->
                        StickerItem(sticker.photo!!, title = "Send sticker") {
                            onSticker(sticker)
                        }
                    }
                }
            }
        }
    } else {
        Div({
            style {
                height(100.percent)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.Center)
                opacity(.5)
            }
        }) {
            Text("You haven't any stickers yet!")
        }
    }
}
