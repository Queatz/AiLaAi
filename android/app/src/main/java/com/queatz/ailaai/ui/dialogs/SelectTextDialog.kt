package com.queatz.ailaai.ui.dialogs

import aiSpeak
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Audio
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.AiSpeakRequest
import io.ktor.http.ContentType
import kotlinx.coroutines.launch

@Composable
fun SelectTextDialog(
    onDismissRequest: () -> Unit,
    text: String,
    autoSpeak: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var audio by rememberStateOf<ByteArray?>(null)
    var isGeneratingAudio by rememberStateOf(false)

    fun speak() {
        if (isGeneratingAudio) {
            return
        }

        scope.launch {
            isGeneratingAudio = true
            api.aiSpeak(AiSpeakRequest(text.notBlank ?: return@launch)) {
                audio = it
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
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SelectionContainer {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                audio?.let { audio ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .padding(1.pad)
                            .clip(MaterialTheme.shapes.large)
                    ) {
                        Audio(audio, ContentType.Audio.OGG.toString(), autoPlay = true)
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                if (audio == null) {
                    IconButton(
                        {
                            speak()
                        }
                    ) {
                        if (isGeneratingAudio) {
                            CircularProgressIndicator(
                                strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth / 2,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(R.string.speak))
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
