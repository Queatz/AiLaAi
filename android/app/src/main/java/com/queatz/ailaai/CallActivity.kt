package com.queatz.ailaai

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.ailaai.api.me
import com.queatz.ailaai.call.CallScreen
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberIsInPipMode
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.db.Person


class CallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterPip()

        onBackPressedDispatcher.addCallback {
            enterPip()
        }

        setContent {
            AiLaAiTheme {
                var me by rememberStateOf<Person?>(null)
                val isInPipMode = rememberIsInPipMode()

                val groupId = remember {
                    when (intent?.action) {
                        Intent.ACTION_CALL -> {
                            intent.getStringExtra(GROUP_ID_EXTRA)
                        }

                        else -> null
                    }

                }

                // todo used cached me for sharing
                LaunchedEffect(Unit) {
                    api.me {
                        me = it
                    }
                }

                if (groupId == null) {
                    finish()
                    return@AiLaAiTheme
                }

                CompositionLocalProvider(LocalAppState provides AppState(me)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isInPipMode) Modifier.clip(MaterialTheme.shapes.large) else Modifier)
                            .background(MaterialTheme.colorScheme.background)
                    ) {

                        if (me != null) {
                            CallScreen(
                                groupId,
                                isInPipMode = isInPipMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

            }
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    private fun enterPip() {
        val ratio = Rational(3, 2)
        enterPictureInPictureMode(
            PictureInPictureParams.Builder().apply {
                setAspectRatio(ratio)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }.build()
        )
    }

    companion object {
        const val GROUP_ID_EXTRA = "groupId"
    }
}
