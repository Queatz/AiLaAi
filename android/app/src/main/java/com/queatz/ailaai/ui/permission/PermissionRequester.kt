@file:OptIn(ExperimentalPermissionsApi::class)

package com.queatz.ailaai.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.*

class PermissionRequester(val permission: String) {

    internal lateinit var state: PermissionState
    private var onPermanentlyDenied: (() -> Unit)? = null
    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null

    fun use(onPermanentlyDenied: () -> Unit = {}, onDenied: () -> Unit = {}, onGranted: () -> Unit) {
        this.onPermanentlyDenied = null
        this.onGranted = null
        this.onDenied = null

        // Special cases for retired permissions
        when (permission) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    onGranted()
                    return
                }
            }
        }

        if (state.status == PermissionStatus.Granted) {
            onGranted()
        } else {
            this.onGranted = onGranted
            this.onDenied = onDenied
            this.onPermanentlyDenied = onPermanentlyDenied
            state.launchPermissionRequest()
        }
    }

    internal fun resolve(isGranted: Boolean) {
        if (isGranted) {
            onGranted?.invoke()
        } else if (!state.status.shouldShowRationale) {
            onPermanentlyDenied?.invoke()
        } else {
            onDenied?.invoke()
        }

        onGranted = null
        onDenied = null
        onPermanentlyDenied = null
    }
}

@Composable
fun permissionRequester(permission: String): PermissionRequester {
    val requester = remember(permission) { PermissionRequester(permission) }

    requester.state = rememberPermissionState(permission) {
        requester.resolve(it)
    }

    return requester
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequester.rememberState() = rememberPermissionState(permission)
