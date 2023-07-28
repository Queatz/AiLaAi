package com.queatz.ailaai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.api.me
import com.queatz.ailaai.api.sendMedia
import com.queatz.ailaai.api.sendMessage
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.dialogs.defaultConfirmPluralFormatter
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AiLaAiTheme {
                val context = LocalContext.current
                val sent = stringResource(R.string.sent)
                var me by rememberStateOf<Person?>(null)

                val content = remember {
                    getIntentContent()
                }

                // todo used cached me for sharing
                LaunchedEffect(Unit) {
                    api.me {
                        me = it
                    }
                }

                if (content == null) {
                    finish()
                    return@AiLaAiTheme
                }

                suspend fun send(groups: List<Group>) {
                    var sendSuccess = false
                    when (content) {
                        is SharedContent.Text -> {
                            coroutineScope {
                                groups.map { group ->
                                    async {
                                        api.sendMessage(
                                            group.id!!,
                                            Message(text = content.text)
                                        ) {
                                            sendSuccess = true
                                        }
                                    }
                                }.awaitAll()
                            }
                        }
                        is SharedContent.Photos -> {
                            coroutineScope {
                                groups.map { group ->
                                    async {
                                        api.sendMedia(
                                            group.id!!,
                                            content.photos,
                                            null
                                        ) {
                                            sendSuccess = true
                                        }
                                    }
                                }.awaitAll()
                            }
                        }
                    }

                    if (sendSuccess) {
                        context.toast(sent)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ChooseGroupDialog(
                        {
                            finish()
                        },
                        title = content.title(),
                        confirmFormatter = content.confirmFormatter(me),
                        me = me
                    ) { groups ->
                        send(groups)
                    }
                }
            }
        }
    }

    private fun getIntentContent(): SharedContent? {
        return when(intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    return handleSendText(intent)
                } else if (intent.type?.startsWith("image/") == true) {
                    return handleSendImage(intent)
                } else {
                    null
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type?.startsWith("image/") == true) {
                    return handleSendMultipleImages(intent)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun handleSendText(intent: Intent): SharedContent? {
        return intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            SharedContent.Text(it)
        }
    }

    private fun handleSendImage(intent: Intent): SharedContent? {
        return (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            SharedContent.Photos(listOf(it))
        }
    }

    private fun handleSendMultipleImages(intent: Intent): SharedContent? {
        return intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
            SharedContent.Photos(it.mapNotNull { it as? Uri })
        }
    }
}

@Composable
private fun SharedContent.confirmFormatter(me: Person?): @Composable (List<GroupExtended>) -> String {
    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    return when (this) {
        is SharedContent.Text -> {
            defaultConfirmFormatter(
                R.string.send_text,
                R.string.send_text_to_group,
                R.string.send_text_to_groups,
                R.string.send_text_to_x_groups
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) }
        }
        is SharedContent.Photos -> {
            defaultConfirmPluralFormatter(
                count = photos.size,
                R.plurals.send_photos,
                R.plurals.send_photos_to_group,
                R.plurals.send_photos_to_groups,
                R.plurals.send_photos_to_x_groups
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) }
        }
    }
}

@Composable
fun SharedContent.title() = when (this) {
    is SharedContent.Text -> stringResource(R.string.send_text)
    is SharedContent.Photos -> pluralStringResource(R.plurals.send_photos, photos.size, photos.size)
}

sealed class SharedContent {
    class Text(val text: String) : SharedContent()
    class Photos(val photos: List<Uri>) : SharedContent()
}
