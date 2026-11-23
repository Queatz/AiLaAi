package com.queatz.ailaai

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
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
import kotlinx.coroutines.flow.collectLatest
import live.videosdk.rtc.android.MediaType
import live.videosdk.rtc.android.VideoSDK
import videosdk.org.webrtc.VideoTrack


class CallActivity : AppCompatActivity() {

    private lateinit var startMediaProjection: ActivityResultLauncher<Intent>
    private var intentGroupId by mutableStateOf<String?>(null)
    private var intentGroupName by mutableStateOf<String?>(null)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateIntentExtras(intent)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        VideoSDK.setActivityForLifeCycle(this)

        startMediaProjection = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                calls.enableScreenShare(result.data)
            } else {
                applicationContext.showDidntWork(R.string.error_share_screen)
            }
        }

        setPictureInPictureParams(pipParams())

        onBackPressedDispatcher.addCallback {
            enterPip()
        }

        setContent {
            AiLaAiTheme {
                val me by calls.me.collectAsState()
                val isInPipMode = rememberIsInPipMode()
                val activeCall by calls.active.collectAsState()
                val enabledStreams by calls.enabledStreams.collectAsState()
                val group by calls.group.collectAsState()
                val cameraPermissionRequester = permissionRequester(Manifest.permission.CAMERA)
                val micPermissionRequester = permissionRequester(Manifest.permission.RECORD_AUDIO)
                var showPermissionDialog by rememberStateOf(false)

                val currentGroupId = intentGroupId ?: group?.group?.id
                val currentGroup = group?.takeIf { currentGroupId == null || it.group?.id == currentGroupId }
                val currentActiveCall = activeCall?.takeIf { currentGroupId == null || it.group.group?.id == currentGroupId }

                LaunchedEffect(Unit) {
                    if (calls.me.value == null) {
                        api.me {
                            calls.me.value = it
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    calls.onEndCall.collectLatest {
                        val activeId = intentGroupId ?: calls.group.value?.group?.id
                        if (it == activeId || activeId == null) {
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

                        if (currentGroup != null || intentGroupName != null || currentActiveCall != null) {
                            CallScreen(
                                group = currentGroup,
                                groupName = intentGroupName.orEmpty(),
                                active = currentActiveCall,
                                isInPipMode = isInPipMode,
                                cameraEnabled = calls.enabled(MediaType.VIDEO, enabledStreams),
                                micEnabled = calls.enabled(MediaType.AUDIO, enabledStreams),
                                screenShareEnabled = calls.enabled(MediaType.SHARE, enabledStreams),
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
                                    calls.end(currentGroupId)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateIntentExtras(intent)
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    private fun updateIntentExtras(intent: Intent?) {
        intent?.getStringExtra(CallService.GROUP_ID_EXTRA)?.let {
            intentGroupId = it
        }
        intent?.getStringExtra(CallService.GROUP_NAME_EXTRA)?.let {
            intentGroupName = it
        }
    }

    private fun pipParams() = PictureInPictureParams.Builder().apply {
        setAspectRatio(Rational(3, 2))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setAutoEnterEnabled(true)
        }
    }.build()

    private fun enterPip() {
        enterPictureInPictureMode(pipParams())
    }
}
