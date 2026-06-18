package com.queatz.ailaai.ui.dialogs

import aiSpeak
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Audio
import com.queatz.ailaai.ui.components.AudioController
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.LoadingIcon
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.AiSpeakRequest
import com.queatz.db.AiSpeakResponse
import com.queatz.db.AiSpeechWord
import kotlinx.coroutines.launch

/**
 * A range of characters in the original text that corresponds to a transcribed word,
 * along with that word's audio timing.
 */
private data class WordSpan(
    val start: Int,
    val end: Int,
    val startMs: Long,
    val endMs: Long
)

/**
 * Maps each transcribed [AiSpeechWord] token to its position in the [text] originally shown to the
 * user, so words can be highlighted and tapped without altering the displayed text.
 */
private fun mapWordsToText(
    text: String,
    words: List<AiSpeechWord>
): List<WordSpan> {
    val spans = mutableListOf<WordSpan>()
    var cursor = 0

    words.forEach { word ->
        val token = word.word?.trim().orEmpty()

        if (token.isEmpty()) {
            return@forEach
        }

        val start = text.indexOf(
            string = token,
            startIndex = cursor,
            ignoreCase = true
        )

        if (start < 0) {
            return@forEach
        }

        val end = start + token.length

        spans.add(
            WordSpan(
                start = start,
                end = end,
                startMs = ((word.start ?: 0.0) * 1000).toLong(),
                endMs = ((word.end ?: 0.0) * 1000).toLong()
            )
        )

        cursor = end
    }

    return spans
}

@Composable
fun SelectTextDialog(
    onDismissRequest: () -> Unit,
    text: String,
    autoSpeak: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var speech by rememberStateOf<AiSpeakResponse?>(null)
    var isGeneratingAudio by rememberStateOf(false)
    var positionMs by rememberStateOf(0L)
    val audioController = remember { AudioController() }

    fun speak() {
        if (isGeneratingAudio) {
            return
        }

        scope.launch {
            isGeneratingAudio = true
            api.aiSpeak(AiSpeakRequest(text.notBlank ?: return@launch)) {
                speech = it
            }
            isGeneratingAudio = false
        }
    }

    LaunchedEffect(autoSpeak) {
        if (autoSpeak) {
            speak()
        }
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                val words = speech?.words?.takeIf { it.isNotEmpty() }
                val wordSpans = remember(text, words) {
                    words?.let { mapWordsToText(text, it) } ?: emptyList()
                }

                if (wordSpans.isEmpty()) {
                    SelectionContainer {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val annotatedText = remember(text, wordSpans, positionMs) {
                        buildAnnotatedString {
                            append(text)
                            wordSpans.forEach { span ->
                                if (positionMs >= span.startMs && positionMs < span.endMs) {
                                    addStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        ),
                                        start = span.start,
                                        end = span.end
                                    )
                                }
                            }
                        }
                    }
                    var layoutResult by rememberStateOf<TextLayoutResult?>(null)

                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(wordSpans, layoutResult) {
                                detectTapGestures { position ->
                                    val offset = layoutResult
                                        ?.getOffsetForPosition(position)
                                        ?: return@detectTapGestures
                                    wordSpans
                                        .firstOrNull { offset in it.start until it.end }
                                        ?.let { audioController.seekTo(it.startMs) }
                                }
                            }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                val audio = speech?.audio

                if (audio == null) {
                    IconButton(
                        onClick = {
                            speak()
                        }
                    ) {
                        if (isGeneratingAudio) {
                            LoadingIcon()
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(R.string.speak))
                        }
                    }
                } else {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .weight(1f)
                            .padding(1.pad)
                            .clip(MaterialTheme.shapes.large)
                    ) {
                        Audio(
                            url = api.url(audio),
                            autoPlay = true,
                            controller = audioController,
                            onPosition = { positionMs = it }
                        )
                    }
                }
                DialogCloseButton(onDismissRequest)
            }
        }
    }
}
