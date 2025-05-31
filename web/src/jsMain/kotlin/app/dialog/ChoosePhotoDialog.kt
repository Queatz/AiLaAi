package app.dialog

import aiPhoto
import aiStyles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.AppStyles
import app.ailaai.api.prompts
import app.ailaai.api.uploadPhotos
import appString
import appText
import application
import com.queatz.db.AiPhotoRequest
import components.IconButton
import focusable
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLImageElement
import org.w3c.files.File
import org.w3c.files.FileReader
import pickPhotos
import r
import toBytes

class ChoosePhotoDialogControl(
    private val scope: CoroutineScope,
    private var aiPrompt: String,
    private val showUpload: Boolean,
    private var aspectRatio: Double? = null,
    private val removeBackground: Boolean = false,
    private val crop: Boolean = false,
) {

    private var _generatingCount = MutableStateFlow(0)
    val generatingCount = _generatingCount.asStateFlow()
    val isGenerating = _generatingCount.map { it > 0 }.stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private var aiStyle = MutableStateFlow<String?>(null)
    private var aiStyles = MutableStateFlow(emptyList<Pair<String, String>>())
    private var count = MutableStateFlow(1)
    private var aspect = MutableStateFlow(aspectRatio)

    fun launch(multiple: Boolean = false, onPhoto: suspend (String, Int?, Int?) -> Unit) {
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
                count = count,
                multiple = multiple,
                showUpload = showUpload,
                onAiStyle = {
                    aiStyle.value = it
                },
                onFile = { photo ->
                    scope.launch {
                        _generatingCount.value++

                        // Get image dimensions using a similar approach to File.toScaledBlob
                        val imageDeferred = CompletableDeferred<HTMLImageElement>()
                        val reader = FileReader()

                        reader.onload = { _ ->
                            val img = document.createElement("img") as HTMLImageElement

                            img.onerror = { _, _, _, _, _ ->
                                imageDeferred.completeExceptionally(Throwable("Error reading image"))
                                Unit
                            }

                            img.onload = { _ ->
                                imageDeferred.complete(img)
                                Unit
                            }

                            img.src = reader.result as String // data url
                            Unit
                        }

                        reader.onerror = { _ ->
                            imageDeferred.completeExceptionally(Throwable("Error reading file"))
                        }

                        reader.readAsDataURL(photo)

                        // Wait for the image to load to get dimensions
                        val img = imageDeferred.await()
                        val width = img.width
                        val height = img.height

                        api.uploadPhotos(
                            listOf(photo.toBytes())
                        ) {
                            onPhoto(it.urls.first(), width, height)
                        }
                        _generatingCount.value--
                    }
                }
            )

            if (!result.isNullOrBlank()) {
                aiPrompt = result
                _generatingCount.value++

                // Generate multiple photos based on count
                val photoCount = count.value

                repeat(photoCount) {
                    api.aiPhoto(
                        AiPhotoRequest(
                            prompt = result,
                            style = aiStyle.value,
                            aspect = aspect.value,
                            removeBackground = removeBackground,
                            crop = crop
                        )
                    ) { photo ->
                        onPhoto(photo.photo, photo.width, photo.height)
                        _generatingCount.value--
                    }
                }
            }
        }
    }
}

@Composable
fun rememberChoosePhotoDialog(
    aiPrompt: String = "",
    showUpload: Boolean = false,
    aspectRatio: Double? = null,
    removeBackground: Boolean = false,
    crop: Boolean = false,
): ChoosePhotoDialogControl {
    val scope = rememberCoroutineScope()

    return remember {
        ChoosePhotoDialogControl(
            scope = scope,
            aiPrompt = aiPrompt,
            showUpload = showUpload,
            aspectRatio = aspectRatio,
            removeBackground = removeBackground,
            crop = crop
        )
    }
}

private suspend fun choosePhotoDialog(
    aiPrompt: String = "",
    aiStyles: StateFlow<List<Pair<String, String>>>,
    aiStyle: StateFlow<String?>,
    count: MutableStateFlow<Int> = MutableStateFlow(1),
    multiple: Boolean = false,
    showUpload: Boolean = false,
    onAiStyle: (String?) -> Unit,
    onFile: (file: File) -> Unit,
): String? {
    // todo: use inputWithListDialog
    val result = inputDialog(
        title = null,
        placeholder = application.appString { describePhoto },
        confirmButton = application.appString { confirm },
        defaultValue = aiPrompt,
        singleLine = false,
        inputStyles = {
            width(32.r)
        },
        inputEndPadding = 9.r,
        inputAction = { resolve, value, onValue ->
            val scope = rememberCoroutineScope()

            // Container for inputs
            Div({
                style {
                    position(Absolute)
                    right(5.r)
                    top(1.r)
                    bottom(1.r)
                    property("display", "flex")
                    property("align-items", "center")
                    property("gap", "0.5rem")
                }
            }) {

                // Number input for photo count (only when multiple is true)
                if (multiple) {
                    Div({
                        style {
                            property("display", "flex")
                            property("flex-direction", "column")
                            property("align-items", "center")
                            property("font-size", "0.75rem")
                        }
                    }) {
                        NumberInput(
                            value = count.collectAsState().value,
                            min = 1,
                            max = 10,
                            attrs = {
                                classes(Styles.dateTimeInput)
                                style {
                                    width(3.r)
                                }

                                onInput {
                                    runCatching {
                                        count.value = (it.value?.toInt() ?: 1).coerceIn(1..10)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            IconButton("expand_more", appString { history }, styles = {
                position(Absolute)
                right(2.5.r)
                top(1.r)
                bottom(1.r)
            }) {
                scope.launch {
                    api.prompts {
                        inputSelectDialog(application.appString { choose }, items = it.map { it.prompt!! }) {
                            onValue(it)
                        }
                    }
                }
            }
        },
        extraButtons = { resolve ->
            if (showUpload) {
                IconButton("photo", application.appString { choosePhoto }) {
                    pickPhotos(multiple = multiple) { files ->
                        onFile(files.first())
                    }
                    resolve(false)
                }
            }
        },
        topContent = { _, _, _ ->
            // Removed NumberInput from here as it's now in inputAction
        }
    ) { _, _, _ ->
        val aiStyles = aiStyles.collectAsState().value
        val aiStyle = aiStyle.collectAsState().value

        Div({
            style {
                overflowY("auto")
                height(8.r)
            }
        }) {
            Div({
                style {
                    property("text-transform", "uppercase")
                    fontSize(14.px)
                    fontWeight("bolder")
                    opacity(.5)
                    paddingLeft(1.r)
                    paddingTop(1.r)
                }
            }) {
                appText { general }
            }
            var previousItem: String? = null

            aiStyles.forEach { (name, style) ->
                if (previousItem != null && previousItem!!.endsWith("HD)") && !name.endsWith("HD)")) {
                    Div({
                        style {
                            property("text-transform", "uppercase")
                            fontSize(14.px)
                            fontWeight("bolder")
                            opacity(.5)
                            paddingLeft(1.r)
                            paddingTop(1.r)
                        }
                    }) {
                        appText { stylized }
                    }
                }

                previousItem = name

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
