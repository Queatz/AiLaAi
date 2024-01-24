@file:OptIn(ExperimentalPermissionsApi::class)

package com.queatz.ailaai.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.*

class PermissionRequester(private val permission: String) {

    internal lateinit var state: PermissionState
    private var onPermanentlyDenied: (() -> Unit)? = null
    private var onGranted: (() -> Unit)? = null

    fun use(onPermanentlyDenied: () -> Unit = {}, onGranted: () -> Unit) {
        require(this.onPermanentlyDenied == null)
        require(this.onGranted == null)

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
        } else if(state.status.shouldShowRationale) {
            onPermanentlyDenied()
        } else {
            this.onGranted = onGranted
            this.onPermanentlyDenied = onPermanentlyDenied
            state.launchPermissionRequest()
        }
    }

    internal fun resolve(isGranted: Boolean) {
        if (isGranted) {
            onGranted?.invoke()
        } else if (state.status.shouldShowRationale) {
            onPermanentlyDenied?.invoke()
        }

        onGranted = null
        onPermanentlyDenied = null
    }
}

@Composable
fun permissionRequester(permission: String): PermissionRequester {
    val requester = PermissionRequester(permission)

    requester.state = rememberPermissionState(permission) {
        requester.resolve(it)
    }

    return requester
}
