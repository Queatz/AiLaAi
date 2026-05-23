package com.queatz.ailaai.ui.dialogs

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.queatz.ailaai.BuildConfig
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Impromptu
import com.queatz.db.ImpromptuLocationUpdates
import app.ailaai.api.myImpromptu
import app.ailaai.api.updateMyImpromptu
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SignalSettingsDialog(
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var impromptu by rememberStateOf<Impromptu?>(null)
    var isLoading by rememberStateOf(true)

    LaunchedEffect(Unit) {
        api.myImpromptu {
            impromptu = it
            isLoading = false
        }
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            Text(
                text = stringResource(R.string.signal_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 2.pad)
            )

            if (isLoading) {
                Loading(modifier = Modifier.padding(vertical = 2.pad))
            } else if (BuildConfig.ENABLE_BACKGROUND_LOCATION) {
                Text(
                    text = stringResource(R.string.background_location),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 1.pad)
                )

                var selectedLocationUpdate by remember(impromptu) {
                    mutableStateOf(impromptu?.updateLocation ?: ImpromptuLocationUpdates.Off)
                }

                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ACCESS_BACKGROUND_LOCATION
                } else {
                    ACCESS_COARSE_LOCATION
                }

                val backgroundLocationPermission = rememberPermissionState(permission)
                val hasBackgroundLocation = backgroundLocationPermission.status == PermissionStatus.Granted

                AnimatedVisibility(selectedLocationUpdate != ImpromptuLocationUpdates.Off && !hasBackgroundLocation) {
                    var showDisclosureDialog by rememberStateOf(false)
                    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) {}

                    OutlinedButton(
                        onClick = {
                            showDisclosureDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad)
                    ) {
                        Text(stringResource(R.string.enable_background_location))
                    }

                    if (showDisclosureDialog) {
                        Alert(
                            onDismissRequest = { showDisclosureDialog = false },
                            title = stringResource(R.string.background_location),
                            text = "Your location is only used to show relevant signals and is not stored except for your last known location.",
                            dismissButton = stringResource(R.string.cancel),
                            confirmButton = stringResource(R.string.accept),
                            onConfirm = {
                                showDisclosureDialog = false
                                backgroundLocationPermissionLauncher.launch(permission)
                            }
                        )
                    }
                }

                ImpromptuLocationUpdates.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = .25f.pad)
                    ) {
                        RadioButton(
                            selected = selectedLocationUpdate == option,
                            onClick = {
                                selectedLocationUpdate = option
                                scope.launch {
                                    api.updateMyImpromptu(Impromptu(updateLocation = option)) {
                                        impromptu = it
                                    }
                                }
                            }
                        )
                        Text(
                            text = when (option) {
                                ImpromptuLocationUpdates.Off -> stringResource(R.string.location_when_open)
                                ImpromptuLocationUpdates.Hourly -> stringResource(R.string.location_hourly)
                                ImpromptuLocationUpdates.Daily -> stringResource(R.string.location_daily)
                                ImpromptuLocationUpdates.Weekly -> stringResource(R.string.location_weekly)
                            },
                            modifier = Modifier.padding(start = 1.pad)
                        )
                    }
                }

                Spacer(Modifier.height(2.pad))
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 2.pad)
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
