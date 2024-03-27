package com.queatz.ailaai.ui.components

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.ui.dialogs.RationaleDialog
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ColumnScope.NotificationsDisabledBanner(show: Boolean = true) {
    val context = LocalContext.current
    val notificationManager = NotificationManagerCompat.from(context)
    var areNotificationsEnabled by rememberStateOf(notificationManager.areNotificationsEnabled())
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    var showPushPermissionDialog by rememberStateOf(false)

    if (showPushPermissionDialog) {
        RationaleDialog(
            {
                showPushPermissionDialog = false
            },
            stringResource(R.string.notifications_disabled_message)
        )
    }

    fun requestNotifications() {
        if (!notificationManager.areNotificationsEnabled()) {
            if (!notificationPermissionState.status.isGranted) {
                if (notificationPermissionState.status.shouldShowRationale) {
                    notificationPermissionState.launchPermissionRequest()
                } else {
                    showPushPermissionDialog = true
                }
            }
        }
    }
    ResumeEffect {
        areNotificationsEnabled = notificationManager.areNotificationsEnabled()
    }

    AnimatedVisibility(!areNotificationsEnabled && show) {
        OutlinedCard(
            onClick = {
                requestNotifications()
            },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.notifications_disabled_message),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(1.pad)
                    .fillMaxWidth()
            )
        }
    }
}
