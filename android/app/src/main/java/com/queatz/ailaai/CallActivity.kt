package com.queatz.ailaai

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import app.ailaai.api.me
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.queatz.ailaai.call.CallScreen
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberIsInPipMode
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.services.calls
import com.queatz.ailaai.ui.dialogs.RationaleDialog
import com.queatz.ailaai.ui.permission.permissionRequester
import com.queatz.ailaai.ui.permission.rememberState
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.db.Person
import kotlinx.coroutines.flow.collectLatest
import live.videosdk.rtc.android.VideoSDK
import org.webrtc.VideoTrack


class CallActivity : AppCompatActivity() {

    private lateinit var startMediaProjection: ActivityResultLauncher<Intent>

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VideoSDK.setActivityForLifeCycle(this)

        startMediaProjection = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                calls.meeting?.enableScreenShare(result.data)
            } else {
                applicationContext.showDidntWork()
            }
        }

        enterPip()

        onBackPressedDispatcher.addCallback {
            enterPip()
        }

        setContent {
            AiLaAiTheme {
                var me by rememberStateOf<Person?>(null)
                val isInPipMode = rememberIsInPipMode()
                val activeCall by calls.active.collectAsState()
                val group by calls.group.collectAsState()
                val cameraPermissionRequester = permissionRequester(Manifest.permission.CAMERA)
                val micPermissionRequester = permissionRequester(Manifest.permission.RECORD_AUDIO)
                var showPermissionDialog by rememberStateOf(false)

                // todo used cached me for calls
                LaunchedEffect(Unit) {
                    api.me {
                        me = it
                    }
                }

                LaunchedEffect(Unit) {
                    calls.onEndCall.collectLatest {
                        if (it == group?.group?.id) {
                            finish()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    calls.onStartScreenShare.collectLatest {
                        startMediaProjection.launch(
                            getSystemService(MediaProjectionManager::class.java)
                                .createScreenCaptureIntent()
                        )
                    }
                }

                LaunchedEffect(
                    micPermissionRequester.rememberState(),
                    cameraPermissionRequester.rememberState()
                ) {
                    if (!micPermissionRequester.state.status.isGranted) {
                        micPermissionRequester.use {
                            calls.toggleMic()
                        }
                    } else if (!cameraPermissionRequester.state.status.isGranted) {
                        cameraPermissionRequester.use {
                            calls.toggleCamera()
                        }
                    }
                }

                CompositionLocalProvider(LocalAppState provides AppState(me)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isInPipMode) Modifier.clip(MaterialTheme.shapes.large) else Modifier)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (showPermissionDialog) {
                            RationaleDialog(
                                {
                                    showPermissionDialog = false
                                },
                                stringResource(R.string.permission_request)
                            )
                        }

                        group?.let { group ->
                            activeCall?.let { active ->
                                CallScreen(
                                    group,
                                    active,
                                    isInPipMode = isInPipMode,
                                    cameraEnabled = calls.enabled("video"),
                                    micEnabled = calls.enabled("audio"),
                                    screenShareEnabled = calls.enabled("share"),
                                    onToggleCamera = {
                                        cameraPermissionRequester.use(
                                            onPermanentlyDenied = {
                                                showPermissionDialog = true
                                            }
                                        ) {
                                            calls.toggleCamera()
                                        }
                                    },
                                    onSwitchCamera = {
                                        cameraPermissionRequester.use(
                                            onPermanentlyDenied = {
                                                showPermissionDialog = true
                                            }
                                        ) {
                                            calls.switchCamera()
                                        }
                                    },
                                    onToggleMic = {
                                        micPermissionRequester.use(
                                            onPermanentlyDenied = {
                                                showPermissionDialog = true
                                            }
                                        ) {
                                            calls.toggleMic()
                                        }
                                    },
                                    onToggleScreenShare = {
                                        calls.toggleScreenShare()
                                    },
                                    onEndCall = {
                                        calls.end(group.group?.id)
                                    },
                                    onTogglePictureInPicture = {
                                        enterPip()
                                    },
                                    onTogglePin = {
                                        calls.togglePin(it.stream as VideoTrack)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
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
}
