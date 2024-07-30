package app.dialog

import aiPhoto
import aiStyles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.AppStyles
import app.ailaai.api.sendMedia
import app.ailaai.api.uploadPhotos
import application
import com.queatz.db.AiPhotoRequest
import components.IconButton
import focusable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.files.File
import pickPhotos
import r
import toBytes

class ChoosePhotoDialogControl(
    private val scope: CoroutineScope,
    private var aiPrompt: String,
    private val showUpload: Boolean
) {

    private var _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private var aiStyle = MutableStateFlow<String?>(null)
    private var aiStyles = MutableStateFlow(emptyList<Pair<String, String>>())

    fun launch(onPhoto: suspend (String) -> Unit) {
        scope.launch {
            if (aiStyles.value.isEmpty()) {
                scope.launch {
                    api.aiStyles {
                        aiStyles.value = it
                    }
                }
            }

            val result = choosePhotoDialog(
                aiPrompt = aiPrompt,
                aiStyles = aiStyles,
                aiStyle = aiStyle,
                showUpload = showUpload,
                onAiStyle = {
                    aiStyle.value = it
                },
                onFile = { photo ->
                    scope.launch {
                        _isGenerating.value = true
                        api.uploadPhotos(
                            listOf(photo.toBytes())
                        ) {
                            onPhoto(it.urls.first())
                        }
                        _isGenerating.value = false
                    }
                }
            )

            if (!result.isNullOrBlank()) {
                aiPrompt = result
                _isGenerating.value = true
                api.aiPhoto(AiPhotoRequest(result, aiStyle.value)) { photo ->
                    onPhoto(photo.photo)
                }
                _isGenerating.value = false
            }
        }
    }
}

@Composable
fun rememberChoosePhotoDialog(
    aiPrompt: String = "",
    showUpload: Boolean = false
): ChoosePhotoDialogControl {
    val scope = rememberCoroutineScope()

    return remember {
        ChoosePhotoDialogControl(scope, aiPrompt, showUpload)
    }
}

private suspend fun choosePhotoDialog(
    aiPrompt: String = "",
    aiStyles: StateFlow<List<Pair<String, String>>>,
    aiStyle: StateFlow<String?>,
    showUpload: Boolean = false,
    onAiStyle: (String?) -> Unit,
    onFile: (file: File) -> Unit
): String? {
    val result = inputDialog(
        title = null,
        placeholder = application.appString { describePhoto },
        confirmButton = application.appString { confirm },
        defaultValue = aiPrompt,
        singleLine = false,
        inputStyles = {
            width(32.r)
        },
        extraButtons = { resolve ->
            if (showUpload) {
                IconButton("photo", application.appString { choosePhoto }) {
                    pickPhotos(multiple = false) { files ->
                        onFile(files.first())
                    }
                    resolve(false)
                }
            }
        }
    ) { _, _, _ ->
        val aiStyles = aiStyles.collectAsState().value
        val aiStyle = aiStyle.collectAsState().value

        Div({
            style {
                marginTop(1.r)
                marginLeft(1.r)
                marginBottom(.5.r)
                opacity(.5f)
                fontSize(14.px)
            }
        }) {
            // todo translate
            Text("Style")
        }
        Div({
            style {
                overflowY("auto")
                height(8.r)
            }
        }) {
            aiStyles.forEach { (name, style) ->
                Div({
                    classes(
                        listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                    )

                    if (aiStyle == style) {
                        classes(AppStyles.groupItemSelected)
                    }

                    onClick {
                        onAiStyle(if (aiStyle == style) null else style)
                    }

                    focusable()
                }) {
                    Div {
                        Div({
                            classes(AppStyles.groupItemName)
                        }) {
                            Text(name)
                        }
                    }
                }
            }
        }
    }

    return result
}
