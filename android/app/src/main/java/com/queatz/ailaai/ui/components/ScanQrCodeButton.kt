package com.queatz.ailaai.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.huawei.hms.hmsscankit.ScanKitActivity
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.dialogs.RationaleDialog
import com.queatz.ailaai.ui.permission.permissionRequester
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val qrCodeExplainedKey = booleanPreferencesKey("tutorial.qrCode.explained")

sealed class ScanQrCodeResult {
    data class Group(val id: String) : ScanQrCodeResult()
    data class Page(val id: String) : ScanQrCodeResult()
    data class Story(val id: String) : ScanQrCodeResult()
    data class Profile(val id: String) : ScanQrCodeResult()
    data class LinkDevice(val token: String) : ScanQrCodeResult()
}

@Composable
fun ScanQrCodeButton() {
    val nav = nav
    ScanQrCodeButton {
        when (it) {
            is ScanQrCodeResult.Group -> {
                nav.navigate("group/${it.id}")
            }
            is ScanQrCodeResult.Page -> {
                nav.navigate("page/${it.id}")
            }
            is ScanQrCodeResult.Story -> {
                nav.navigate("story/${it.id}")
            }
            is ScanQrCodeResult.Profile -> {
                nav.navigate("profile/${it.id}")
            }
            is ScanQrCodeResult.LinkDevice -> {
                nav.navigate("link-device/${it.token}")
            }
        }
    }
}

@Composable
fun ScanQrCodeButton(onResult: (ScanQrCodeResult) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showQrCodeExplanationDialog by rememberStateOf(false)
    var showCameraRationale by rememberStateOf(false)
    val cameraPermissionRequester = permissionRequester(Manifest.permission.CAMERA)
    val nav = nav

    val scanQrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getParcelableExtra<HmsScan?>(ScanUtil.RESULT)
                ?.let {
                    it.linkUrl?.linkValue?.takeIf { it.startsWith(appDomain) }?.drop(appDomain.length)?.let {
                        when {
                            it.startsWith("/group/") -> {
                                val groupId = it.split("/").getOrNull(2) ?: return@let null
                                onResult(ScanQrCodeResult.Group(groupId))
                                true
                            }
                            it.startsWith("/page/") -> {
                                val cardId = it.split("/").getOrNull(2) ?: return@let null
                                onResult(ScanQrCodeResult.Page(cardId))
                                true
                            }
                            // Depreciated
                            it.startsWith("/card/") -> {
                                val cardId = it.split("/").getOrNull(2) ?: return@let null
                                onResult(ScanQrCodeResult.Page(cardId))
                                true
                            }
                            it.startsWith("/story/") -> {
                                val storyId = it.split("/").getOrNull(2) ?: return@let null
                                onResult(ScanQrCodeResult.Story(storyId))
                                true
                            }
                            it.startsWith("/profile/") -> {
                                val personId = it.split("/").getOrNull(2) ?: return@let null
                                onResult(ScanQrCodeResult.Profile(personId))
                                true
                            }
                            it.startsWith("/link-device/") -> {
                                val token = it.split("/").getOrNull(2) ?: return@let null
                                onResult(ScanQrCodeResult.LinkDevice(token))
                                true
                            }
                            else -> null
                        }
                    }
                } ?: run {
                context.showDidntWork()
            }
        }
    }

    fun scanQrCode() {
        cameraPermissionRequester.use(
            onPermanentlyDenied = {
                showCameraRationale = true
            }
        ) {
            scanQrLauncher.launch(
                // https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/android-parsing-result-codes-0000001050043969
                // Extracted from ScanUtil.java (startScan)
                Intent(nav.context as Activity, ScanKitActivity::class.java).apply {
                    putExtra("ScanFormatValue", HmsScan.QRCODE_SCAN_TYPE)
                    putExtra("ScanViewValue", 1)
                }
            )
        }
    }

    fun launchScanQrCode() {
        scope.launch {
            if (context.dataStore.data.first()[qrCodeExplainedKey] == true) {
                scanQrCode()
            } else {
                showQrCodeExplanationDialog = true
            }
        }
    }

    if (showCameraRationale) {
        RationaleDialog(
            {
                showCameraRationale = false
            },
            stringResource(R.string.camera_disabled_description)
        )
    }

    if (showQrCodeExplanationDialog) {
        AlertDialog(
            {
                showQrCodeExplanationDialog = false
            },
            title = {
                Text(stringResource(R.string.scan))
            },
            text = {
                Text(stringResource(R.string.scan_a_qr_code_description))
            },
            confirmButton = {
                TextButton(
                    {
                        scope.launch {
                            context.dataStore.edit {
                                it[qrCodeExplainedKey] = true
                            }
                            showQrCodeExplanationDialog = false
                            scanQrCode()
                        }
                    }
                ) {
                    Text(stringResource(R.string.scan_now))
                }
            },
            dismissButton = {
                TextButton(
                    {
                        showQrCodeExplanationDialog = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    IconButton({
        launchScanQrCode()
    }) {
        Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan))
    }
}
