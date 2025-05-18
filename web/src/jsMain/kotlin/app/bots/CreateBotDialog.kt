package app.bots

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createBot
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.photoDialog
import app.dialog.rememberChoosePhotoDialog
import app.menu.Menu
import application
import baseUrl
import com.queatz.db.Bot
import com.queatz.db.BotData
import com.queatz.db.BotDetailsBody
import components.GroupPhoto
import components.GroupPhotoItem
import components.IconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r

suspend fun createBotDialog(
    scope: CoroutineScope,
    onBotHelp: () -> Unit,
    onBotCreated: (Bot) -> Unit
) {
    val secret = MutableStateFlow("")
    val photo = MutableStateFlow("")

    val url = inputDialog(
        title = application.appString { createBot },
        placeholder = "https://",
        confirmButton = application.appString { create },
        extraButtons = {
            // todo: translate
            IconButton("help", "Bot specifications") {
                onBotHelp()
            }
        },
        actions = {
            val choosePhoto = rememberChoosePhotoDialog(showUpload = true)

            var menuTarget by remember { mutableStateOf<DOMRect?>(null) }

            menuTarget?.let {
                Menu({ menuTarget = null }, it) {
                    item(application.appString { this.choosePhoto }) {
                        scope.launch {
                            choosePhoto.launch { it, _, _ ->
                                photo.value = it
                            }
                        }
                    }
                    // todo: translate
                    item("Secret") {
                        scope.launch {
                            val result = botSecretDialog(secret.value)

                            if (result != null) {
                                secret.value = result
                            }
                        }
                    }
                }
            }

            val isGenerating = choosePhoto.isGenerating.collectAsState().value

            // todo: translate
            IconButton("more_vert", "More options", isLoading = isGenerating) {
                menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
            }
        },
        topContent = { _, _, _ ->
            val photo = photo.collectAsState().value

            if (photo.isNotBlank()) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        alignItems(AlignItems.Center)
                        marginBottom(1.r)
                    }
                }) {
                    GroupPhoto(
                        listOf(GroupPhotoItem(photo, null)),
                        size = 84.px
                    ) {
                        scope.launch {
                            photoDialog("$baseUrl$photo")
                        }
                    }
                }
            }
        }
    )

    if (url != null) {
        api.createBot(
            BotDetailsBody(
                url = url,
                photo = photo.value,
                data = secret.value.notBlank?.let { BotData(secret = it) }
            ),
            onError = {
                scope.launch { dialog(application.appString { didntWork }, cancelButton = null) }
            }
        ) {
            onBotCreated(it)
        }
    }
}
