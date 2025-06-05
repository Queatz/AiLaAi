package app.components

import Styles
import aiTranscribe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.dialog.rememberChoosePhotoDialog
import com.queatz.db.AiTranscribeResponse
import components.IconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.CSSUnit
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontFamily
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.dom.TextInput
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import org.w3c.files.File
import r
import resize
import toBytes
import web.events.EventHandler
import web.media.recorder.BlobEvent
import web.media.recorder.MediaRecorder
import web.media.streams.MediaStream
import web.media.streams.MediaStreamConstraints
import web.media.streams.MediaTrackConstraints
import web.navigator.navigator

class FlexInputControl {
    var requestFocus: () -> Unit = {}

    fun focus() {
        requestFocus()
    }
}

@Composable
fun FlexInput(
    value: String = "",
    onChange: (String) -> Unit = {},
    control: FlexInputControl = remember { FlexInputControl() },
    initialValue: String = remember { value },
    placeholder: String = "",
    // Input configuration
    singleLine: Boolean = false,
    inputType: InputType<*>? = null,
    autoFocus: Boolean = false,
    selectAll: Boolean = false,
    enabled: Boolean = true,
    autoSize: Boolean = true,
    monospace: Boolean = false,
    // Style configuration
    styles: StyleScope.() -> Unit = {},
    defaultMargins: Boolean = false,
    useDefaultWidth: Boolean = true,
    // Feature toggles
    enableVoiceInput: Boolean = true,
    enablePhotoPasting: Boolean = false,
    enablePhotoUpload: Boolean = false,
    // Action handlers
    onSubmit: suspend () -> Boolean = { false },
    onDismissRequest: () -> Unit = {},
    onPhotoFiles: (suspend (List<File>) -> Unit)? = null,
    onPhotos: (suspend (List<Triple<String, Int?, Int?>>) -> Unit)? = null,
    // Button configuration
    showButtons: Boolean = false,
    buttonText: String = "Submit",
    showDiscard: Boolean = true,
    discardText: String = "Discard",
    buttonStyles: StyleScope.() -> Unit = {},
    inputEndPadding: CSSSizeValue<CSSUnit.rem> = if (enableVoiceInput) 3.r else 0.r
) {
    val scope = rememberCoroutineScope()

    // State variables
    val initialValue = remember(initialValue) { initialValue }
    var isRecording by remember { mutableStateOf(false) }
    var isTranscribingAudio by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var stream by remember { mutableStateOf<MediaStream?>(null) }
    var focus by remember { mutableStateOf<(() -> Unit)?>(null) }
    var valueChanged by remember { mutableStateOf(false) }
    var textareaRef by remember { mutableStateOf<HTMLTextAreaElement?>(null) }

    // Photo dialog if photo upload is enabled
    val photoDialog = if (enablePhotoUpload && onPhotos != null) {
        rememberChoosePhotoDialog(showUpload = true)
    } else null

    // Handle value changes
    LaunchedEffect(value) {
        valueChanged = value != initialValue
    }

    // Voice recording functions
    fun startRecording() {
        if (isRecording || !enableVoiceInput) return

        scope.launch {
            val constraints = MediaStreamConstraints(
                audio = MediaTrackConstraints(
                    echoCancellation = true,
                    noiseSuppression = true
                )
            )

            try {
                stream = navigator.mediaDevices.getUserMedia(constraints)
                recorder = MediaRecorder(stream!!)

                recorder?.start()
                isRecording = true
            } catch (e: Exception) {
                console.error("Error starting recording", e)
            }
        }
    }

    fun stopRecording(isCancel: Boolean = false, onComplete: (() -> Unit)? = null) {
        if (!isRecording || !enableVoiceInput) return

        recorder?.let { rec ->
            if (isCancel) {
                rec.ondataavailable = null
                stream?.getTracks()?.forEach {
                    it.stop()
                }
            } else {
                rec.ondataavailable = EventHandler { event: BlobEvent ->
                    val blob = event.data
                    val file = File(arrayOf(blob), "recording.webm")

                    isTranscribingAudio = true

                    scope.launch {
                        try {
                            val bytes = file.toBytes()

                            api.aiTranscribe(
                                audio = bytes,
                                onError = { error: Throwable ->
                                    console.error("Error transcribing audio: ${error.message}")
                                    isTranscribingAudio = false
                                }
                            ) { response: AiTranscribeResponse ->
                                response.text.let { text ->
                                    if (text.isNotBlank()) {
                                        onChange(if (value.isNotBlank()) "$value $text" else text)
                                        focus?.invoke()
                                        onComplete?.invoke()
                                    }
                                }
                                isTranscribingAudio = false
                            }
                        } catch (e: Exception) {
                            console.error("Error processing audio: ${e.message}")
                            isTranscribingAudio = false
                        }
                    }
                }
            }
            rec.stop()
            stream?.getTracks()?.forEach { it.stop() }

            recorder = null
            stream = null
            isRecording = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopRecording(isCancel = true)
        }
    }

    if (autoSize) {
        LaunchedEffect(value) {
            delay(1)
            textareaRef?.resize()
        }
    }

    // Main container
    Div({
        style {
            position(Position.Relative)
            display(DisplayStyle.Flex)
            if (defaultMargins) margin(.5.r, 1.r)
            else width(100.percent)
            styles()
        }
    }) {
        // Choose the appropriate input component based on singleLine
        if (singleLine) {
            TextInput(value) {
                classes(Styles.textarea)

                inputType?.let {
                    type(it)
                }

                style {
                    flex(1)
                    paddingRight(inputEndPadding)
                    maxWidth(100.percent)
                    if (!useDefaultWidth) {
                        width(0.r)
                    }
                }

                placeholder(placeholder)

                if (!enabled) {
                    disabled()
                }

                if (autoFocus) {
                    autoFocus()
                }

                onKeyDown {
                    when (it.key) {
                        "Enter" -> {
                            it.preventDefault()
                            it.stopPropagation()
                            scope.launch {
                                if (onSubmit()) {
                                    valueChanged = false
                                }
                            }
                        }

                        "Escape" -> {
                            it.preventDefault()
                            it.stopPropagation()
                            if (isRecording) {
                                stopRecording(isCancel = true)
                            } else {
                                onDismissRequest()
                            }
                        }
                    }
                }

                onInput {
                    onChange(it.value)
                }

                onChange {
                    onChange(it.value)
                }

                ref { element ->
                    if (autoFocus) {
                        element.focus()
                    }

                    if (selectAll) {
                        element.select()
                    }

                    control.requestFocus = {
                        element.focus()
                    }

                    focus = { element.focus() }

                    onDispose {
                        control.requestFocus = {}
                        focus = null
                    }
                }
            }
        } else {
            TextArea(value) {
                classes(Styles.textarea)

                style {
                    height(3.5.r)
                    if (!useDefaultWidth) {
                        width(0.r)
                    }
                    if (autoSize) {
                        minHeight(3.5.r)
                        maxHeight(18.r)
                    }
                    flex(1)
                    backgroundColor(Color.transparent)

                    if (monospace) {
                        fontFamily("monospace")
                    }
                    paddingRight(inputEndPadding)
                }

                placeholder(placeholder)

                if (!enabled) {
                    disabled()
                }

                onKeyDown {
                    when {
                        it.key == "Enter" && !it.shiftKey -> {
                            it.preventDefault()
                            it.stopPropagation()
                            scope.launch {
                                if (onSubmit()) {
                                    valueChanged = false
                                }
                                delay(1)
                                (it.target as HTMLTextAreaElement).resize()
                            }
                        }

                        it.key == "Escape" -> {
                            it.preventDefault()
                            it.stopPropagation()
                            if (isRecording) {
                                stopRecording(isCancel = true)
                            } else {
                                onDismissRequest()
                            }
                        }
                    }
                }

                onInput {
                    onChange(it.value)
                    if (autoSize) {
                        it.target.resize()
                    }
                }

                onChange {
                    onChange(it.value)
                    if (autoSize) {
                        it.target.resize()
                    }
                }

                if (enablePhotoPasting) {
                    onPaste {
                        if (onPhotoFiles == null) return@onPaste

                        val items = it.clipboardData?.items ?: return@onPaste

                        val photos = (0 until items.length).mapNotNull {
                            items[it]
                        }.filter {
                            it.type.startsWith("image/")
                        }.mapNotNull {
                            it.getAsFile()
                        }

                        if (photos.isEmpty()) return@onPaste

                        scope.launch {
                            onPhotoFiles(photos)
                        }

                        it.preventDefault()
                    }
                }

                if (autoFocus) {
                    autoFocus()
                }

                ref { element ->
                    textareaRef = element

                    if (autoFocus) {
                        element.focus()
                    }

                    if (autoSize) {
                        element.resize()
                    }

                    if (selectAll) {
                        element.select()
                    }

                    control.requestFocus = {
                        element.focus()
                    }

                    focus = { element.focus() }

                    onDispose {
                        textareaRef = null
                        control.requestFocus = {}
                        focus = null
                    }
                }
            }
        }

        // Voice input button
        if (enableVoiceInput) {
            IconButton(
                name = if (isRecording) "stop" else "mic",
                title = if (isRecording) "Finish voice input" else "Voice input",
                isLoading = !isRecording && isTranscribingAudio,
                enabled = enabled,
                styles = {
                    position(Position.Absolute)
                    right(.5.r)
                    top(.5.r)
                    bottom(.5.r)
                    opacity(.5f)
                }
            ) {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
                focus?.invoke()
            }
        }

        // Photo upload button
        if (enablePhotoUpload && onPhotos != null) {
            IconButton(
                name = "photo",
                title = "Upload photo",
                enabled = enabled,
                styles = {
                    position(Position.Absolute)
                    right(if (enableVoiceInput) 2.r else .5.r)
                    top(.5.r)
                    bottom(.5.r)
                    opacity(.5f)
                }
            ) {
                photoDialog?.launch(
                    multiple = false,
                    onPhoto = { photo, width, height ->
                        onPhotos(listOf(Triple(photo, width, height)))
                    }
                )
                focus?.invoke()
            }
        }
    }

    // Button bar for submit/discard
    if (showButtons && valueChanged) {
        Div({
            style {
                if (defaultMargins) {
                    margin(.5.r, 1.r)
                } else {
                    margin(.5.r, 0.r)
                }
                flexShrink(0)
                display(DisplayStyle.Flex)
                buttonStyles()
                gap(.5.r)
            }
        }) {
            var isSubmitting by remember { mutableStateOf(false) }
            Button({
                classes(Styles.button)

                onClick {
                    scope.launch {
                        isSubmitting = true
                        if (onSubmit()) {
                            valueChanged = false
                        }
                        isSubmitting = false
                    }
                }

                if (!enabled || isSubmitting) {
                    disabled()
                }
            }) {
                Text(buttonText)
            }

            if (showDiscard) {
                Button({
                    classes(Styles.outlineButton)

                    onClick {
                        onChange(initialValue)
                    }

                    if (!enabled) {
                        disabled()
                    }
                }) {
                    Text(discardText)
                }
            }
        }
    }
}
