package com.queatz.ailaai.impromptu

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ailaai.api.createImpromptuSeek
import app.ailaai.api.deleteImpromptuHistory
import app.ailaai.api.deleteImpromptuSeek
import app.ailaai.api.getImpromptuHistory
import app.ailaai.api.myImpromptu
import app.ailaai.api.updateImpromptuSeek
import app.ailaai.api.updateMyImpromptu
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.formatDistance
import com.queatz.ailaai.extensions.formatFuture
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.dialogs.DialogHeader
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Impromptu
import com.queatz.db.ImpromptuHistory
import com.queatz.db.ImpromptuLocationUpdates
import com.queatz.db.ImpromptuMode
import com.queatz.db.ImpromptuNotificationStyle
import com.queatz.db.ImpromptuSeek
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun ImpromptuDialog(
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var impromptu by rememberStateOf<Impromptu?>(null)
    var isLoading by rememberStateOf(true)

    // Load current impromptu settings
    LaunchedEffect(Unit) {
        api.myImpromptu {
            impromptu = it
        }
        isLoading = false
    }

    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            content = {
                // Dialog header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.impromptu_mode),
                        style = MaterialTheme.typography.titleLarge
                    )
                    var showHistoryDialog by rememberStateOf(false)

                    IconButton(
                        onClick = {
                            showHistoryDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.history)
                        )
                    }

                    if (showHistoryDialog) {
                        ImpromptuHistoryDialog(
                            onDismissRequest = { showHistoryDialog = false }
                        )
                    }
                }

                if (isLoading) {
                    Loading(
                        modifier = Modifier.padding(vertical = 2.pad)
                    )
                    return@DialogLayout
                }

                Spacer(modifier = Modifier.height(1.pad))

                // Mode selector
                var selectedMode by rememberSaveable(impromptu) { mutableStateOf(impromptu?.mode ?: ImpromptuMode.Off) }

                Spacer(modifier = Modifier.height(1.pad))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    ImpromptuMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                scope.launch {
                                    api.updateMyImpromptu(
                                        Impromptu(mode = mode)
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(
                                when (mode) {
                                    ImpromptuMode.Off -> stringResource(R.string.off)
                                    ImpromptuMode.Friends -> stringResource(R.string.friends)
                                    ImpromptuMode.Everyone -> stringResource(R.string.everyone)
                                }
                            )
                            }
                        )
                    }
                }

                if (selectedMode == ImpromptuMode.Off) {
                    Text(
                        text = stringResource(R.string.impromptu_connect_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(1.pad))
                    Text(
                        text = stringResource(R.string.impromptu_nearby_notification),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(1.pad))
                    Text(
                        text = stringResource(R.string.impromptu_seeking_visibility),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.pad))

                    // I'm seeking section
                    Text(
                        text = stringResource(R.string.im_seeking),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(1.pad))

                    var seekItems by rememberStateOf(impromptu?.seek ?: emptyList())

                    seekItems.forEach { item ->
                        ImpromptuSeekItem(
                            item = item,
                            onUpdate = {
                                // Refresh data after update
                                scope.launch {
                                    api.myImpromptu {
                                        impromptu = it
                                        seekItems = it.seek ?: emptyList()
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    api.deleteImpromptuSeek(item.id!!) {
                                        // todo reload
                                        seekItems -= item
                                    }
                                }
                            }
                        )
                    }

                    var showAddSeekDialog by rememberStateOf(false)

                    OutlinedButton(
                        onClick = {
                            showAddSeekDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(1.pad))
                        Text(stringResource(R.string.add))
                    }

                    if (showAddSeekDialog) {
                        ImpromptuItemDialog(
                            isSeek = true,
                            onDismissRequest = { showAddSeekDialog = false },
                            onSave = { name, radius, expiresAt ->
                                scope.launch {
                                    api.createImpromptuSeek(
                                        ImpromptuSeek(
                                            name = name,
                                            radius = radius,
                                            expiresAt = expiresAt
                                        )
                                    ) {
                                        // todo reload
                                        seekItems += it
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.pad))

                    // I'm providing section
                    Text(
                        text = stringResource(R.string.im_providing),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(1.pad))

                    var offeringItems by rememberStateOf(impromptu?.offer ?: emptyList())

                    offeringItems.forEach { item ->
                        ImpromptuSeekItem(
                            item = item,
                            onUpdate = {
                                // Refresh data after update
                                scope.launch {
                                    api.myImpromptu {
                                        impromptu = it
                                        offeringItems = it.offer ?: emptyList()
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    api.deleteImpromptuSeek(item.id!!) {
                                        // todo reload
                                        offeringItems -= item
                                    }
                                }
                            }
                        )
                    }

                    var showAddOfferingDialog by rememberStateOf(false)

                    OutlinedButton(
                        onClick = {
                            showAddOfferingDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(1.pad))
                        Text(stringResource(R.string.add))
                    }

                    if (showAddOfferingDialog) {
                        ImpromptuItemDialog(
                            isSeek = false,
                            onDismissRequest = { showAddOfferingDialog = false },
                            onSave = { name, radius, expiresAt ->
                                scope.launch {
                                    api.createImpromptuSeek(
                                        ImpromptuSeek(
                                            name = name,
                                            radius = radius,
                                            expiresAt = expiresAt,
                                            offer = true
                                        )
                                    ) {
                                        // todo reload
                                        offeringItems += it
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.pad))

                    // Notification info
                    Text(
                        text = stringResource(R.string.impromptu_nearby_notification),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.pad))

                    var showSettingsDialog by rememberStateOf(false)

                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(1.pad))
                        Text(stringResource(R.string.more_settings))
                    }

                    if (showSettingsDialog) {
                        ImpromptuSettingsDialog(
                            onDismissRequest = { showSettingsDialog = false },
                            impromptu = impromptu
                        ) {
                            impromptu = it
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}

@Composable
private fun ImpromptuHistoryDialog(
    onDismissRequest: () -> Unit
) {
    val nav = nav
    val scope = rememberCoroutineScope()
    var historyItems by rememberStateOf<List<ImpromptuHistory>>(emptyList())
    var isLoading by rememberStateOf(true)

    // Load history items
    LaunchedEffect(Unit) {
        api.getImpromptuHistory {
            historyItems = it
            isLoading = false
        }
    }

    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            content = {
                // Dialog header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.impromptu_history),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(2.pad))

                if (isLoading) {
                    Loading(
                        modifier = Modifier.padding(vertical = 2.pad)
                    )
                    return@DialogLayout
                }

                if (historyItems.isEmpty()) {
                    EmptyText(
                        text = stringResource(R.string.no_history_yet),
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        historyItems.forEach { historyItem ->
                            ImpromptuHistoryItem(
                                item = historyItem,
                                onClick = {
                                    nav.appNavigate(
                                        AppNav.Profile(historyItem.otherPerson!!)
                                    )
                                },
                                onDelete = {
                                    scope.launch {
                                        api.deleteImpromptuHistory(historyItem.id!!) {
                                            historyItems = historyItems - historyItem
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}

@Composable
private fun ImpromptuHistoryItem(
    item: ImpromptuHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDeleteAlert by remember(item) {
        mutableStateOf(false)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable {
                onClick()
            }
    ) {
        item.otherPersonDetails?.let { person ->
            GroupPhoto(
                photos = listOf(item.otherPersonDetails!!.contactPhoto()),
                size = 32.dp
            )
        }
        TextAndDescription(
            text = item.otherPersonDetails?.name ?: stringResource(R.string.someone),
            description = bulletedString(
                item.createdAt!!.timeAgo(),
                (item.distance ?: 0.0).formatDistance(),
                item.seeksDetails?.firstOrNull { it.offer == true }?.name
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = .5f.pad)
        )

        if (confirmDeleteAlert) {
            Alert(
                onDismissRequest = { confirmDeleteAlert = false },
                title = stringResource(R.string.delete_this_history),
                dismissButton = stringResource(R.string.cancel),
                confirmButton = stringResource(R.string.delete),
                confirmColor = MaterialTheme.colorScheme.error,
                onConfirm = onDelete
            )
        }

        IconButton(
            onClick = {
                confirmDeleteAlert = true
            },
            modifier = Modifier.padding(start = 1.pad)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ImpromptuSettingsDialog(
    onDismissRequest: () -> Unit,
    impromptu: Impromptu?,
    onImpromptu: (Impromptu) -> Unit,
) {
    val scope = rememberCoroutineScope()

    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            content = {
                DialogHeader(stringResource(R.string.impromptu_settings))

                Spacer(modifier = Modifier.height(2.pad))

                // Update location section
                Text(
                    text = stringResource(R.string.update_my_location),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(1.pad))

                var selectedLocationUpdate by rememberSaveable {
                    mutableStateOf(impromptu?.updateLocation ?: ImpromptuLocationUpdates.Off)
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                        ) {
                            Text(stringResource(R.string.enable_background_location))
                        }

                        if (showDisclosureDialog) {
                            Alert(
                                onDismissRequest = { showDisclosureDialog = false },
                                // todo: translate
                                title = "Background location",
                                // todo: translate
                                text = "Your location is not saved except for the last location received in order to power the Impromptu feature.",
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
                                .padding(vertical = .5f.pad)
                        ) {
                            RadioButton(
                                selected = selectedLocationUpdate == option,
                                onClick = {
                                    selectedLocationUpdate = option
                                    scope.launch {
                                        api.updateMyImpromptu(Impromptu(updateLocation = option)) {
                                            onImpromptu(it)
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
                }

                Spacer(modifier = Modifier.height(2.pad))

                // Notification style section
                Text(
                    text = "Notification mode",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(1.pad))

                var selectedNotificationStyle by rememberSaveable {
                    mutableStateOf(impromptu?.notificationType ?: ImpromptuNotificationStyle.Normal)
                }

                Column {
                    ImpromptuNotificationStyle.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedNotificationStyle == option,
                                onClick = {
                                    selectedNotificationStyle = option
                                    scope.launch {
                                        api.updateMyImpromptu(Impromptu(notificationType = option)) {
                                            onImpromptu(it)
                                        }
                                    }
                                }
                            )
                            TextAndDescription(
                                text = when (option) {
                                    ImpromptuNotificationStyle.Normal -> stringResource(R.string.notification_normal)
                                    ImpromptuNotificationStyle.Passive -> stringResource(R.string.notification_passive)
                                },
                                description = when (option) {
                                    ImpromptuNotificationStyle.Normal -> stringResource(R.string.notification_normal_description)
                                    ImpromptuNotificationStyle.Passive -> stringResource(R.string.notification_passive_description)
                                },
                                modifier = Modifier.padding(start = 1.pad)
                            )
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}

@Composable
fun TextAndDescription(
    text: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = text
        )
        description?.notBlank?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(.5f)
            )
        }
    }
}

@Composable
private fun ImpromptuSeekItem(
    item: ImpromptuSeek,
    onUpdate: () -> Unit,
    onDelete: (String) -> Unit
) {
    var showExtendMenu by rememberStateOf(false)
    var showEditRadiusDialog by rememberStateOf(false)
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        TextAndDescription(
            text = item.name.orEmpty(),
            description = stringResource(
                R.string.distance_and_expiry,
                (item.radius ?: 5.0).format(),
                if (item.expiresAt == null) {
                    stringResource(R.string.never_expires)
                } else {
                    stringResource(
                        if (item.expiresAt?.let { it <= Clock.System.now() } == true) {
                            R.string.expired_x
                        } else {
                            R.string.expires_x
                        },
                        item.expiresAt?.formatFuture().orEmpty()
                    )
                }
            ),
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    showExtendMenu = true
                }
                .padding(horizontal = 1.pad, vertical = .5f.pad)
        )

        var confirmDeleteAlert by remember(item) {
            mutableStateOf(false)
        }

        if (confirmDeleteAlert) {
            Alert(
                onDismissRequest = { confirmDeleteAlert = false },
                title = stringResource(
                    if (item.offer == true) {
                        R.string.stop_providing_this
                    } else {
                        R.string.stop_seeking_this
                    }
                ),
                dismissButton = stringResource(R.string.cancel),
                confirmButton = stringResource(R.string.yes_delete),
                confirmColor = MaterialTheme.colorScheme.error,
                onConfirm = {
                    scope.launch {
                        item.id?.let { seekId ->
                            onDelete(seekId)
                        }
                    }
                }
            )
        }

        IconButton(
            onClick = {
                confirmDeleteAlert = true
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showEditRadiusDialog) {
        TextFieldDialog(
            onDismissRequest = { showEditRadiusDialog = false },
            title = stringResource(R.string.edit_search_distance),
            button = stringResource(R.string.save),
            initialValue = (item.radius ?: 5.0).toString(),
            placeholder = "5.0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            valueFormatter = { value: String ->
                // Only allow numbers and decimal point
                if (value.isEmpty() || value.all { char: Char -> char.isDigit() || char == '.' }) {
                    value
                } else {
                    null
                }
            },
            onSubmit = { value: String ->
                val newRadius = value.toDoubleOrNull() ?: 5.0
                item.id?.let { seekId ->
                    scope.launch {
                        val updatedSeek = item.copy(
                            radius = newRadius
                        )
                        api.updateImpromptuSeek(seekId, updatedSeek) {
                            onUpdate()
                        }
                    }
                }
                showEditRadiusDialog = false
            }
        )
    }

    if (showExtendMenu) {
        Menu(
            onDismissRequest = { showExtendMenu = false }
        ) {
            menuItem(
                title = stringResource(R.string.extend_by_1_hour),
                action = {
                    item.id?.let { seekId ->
                        scope.launch {
                            val updatedSeek = item.copy(
                                expiresAt = (item.expiresAt ?: Clock.System.now()) + 1.hours
                            )
                            api.updateImpromptuSeek(seekId, updatedSeek) {
                                onUpdate()
                            }
                        }
                    }
                    showExtendMenu = false
                }
            )
            menuItem(
                title = stringResource(R.string.extend_by_1_day),
                action = {
                    item.id?.let { seekId ->
                        scope.launch {
                            val updatedSeek = item.copy(
                                expiresAt = (item.expiresAt ?: Clock.System.now()) + 1.days
                            )
                            api.updateImpromptuSeek(seekId, updatedSeek) {
                                onUpdate()
                            }
                        }
                    }
                    showExtendMenu = false
                }
            )
            menuItem(
                title = stringResource(R.string.extend_by_1_week),
                action = {
                    item.id?.let { seekId ->
                        scope.launch {
                            val updatedSeek = item.copy(
                                expiresAt = (item.expiresAt ?: Clock.System.now()) + 7.days
                            )
                            api.updateImpromptuSeek(seekId, updatedSeek) {
                                onUpdate()
                            }
                        }
                    }
                    showExtendMenu = false
                }
            )
            menuItem(
                title = stringResource(R.string.extend_by_1_year),
                action = {
                    item.id?.let { seekId ->
                        scope.launch {
                            val updatedSeek = item.copy(
                                expiresAt = (item.expiresAt ?: Clock.System.now()) + 365.days
                            )
                            api.updateImpromptuSeek(seekId, updatedSeek) {
                                onUpdate()
                            }
                        }
                    }
                    showExtendMenu = false
                }
            )
            menuItem(
                title = stringResource(R.string.edit_search_distance),
                action = {
                    showEditRadiusDialog = true
                    showExtendMenu = false
                }
            )
        }
    }
}

enum class ExpirationOption(
    @StringRes val label: Int,
    val duration: Duration
) {
    Hour(R.string._1_hour, 1.hours),
    Day(R.string._1_day, 1.days),
    Week(R.string._1_week, 7.days),
    Month(R.string._1_month, 30.days),
    Year(R.string._1_year, 365.days);
}

@Composable
private fun ImpromptuItemDialog(
    isSeek: Boolean,
    onDismissRequest: () -> Unit,
    onSave: (String, Double, kotlin.time.Instant) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var distance by rememberSaveable { mutableStateOf("5") }
    var expiration by rememberSaveable { mutableStateOf(ExpirationOption.Month) }


    var showExpirationOptions by rememberStateOf(false)

    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            content = {
                DialogHeader(if (isSeek) stringResource(R.string.im_seeking) else stringResource(R.string.im_providing))

                Spacer(modifier = Modifier.height(2.pad))

                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.flute_teacher_example)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(2.pad))

                // Distance field
                OutlinedTextField(
                    value = distance,
                    onValueChange = {
                        // Only allow numbers
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            distance = it.toIntOrNull()
                                ?.coerceAtMost(10)
                                ?.toString()
                                .orEmpty()
                        }
                    },
                    label = { Text(stringResource(R.string.search_distance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.pad))

                // Expiration field
                OutlinedButton(
                    onClick = { showExpirationOptions = true },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.expires_with_time, stringResource(expiration.label)),
                        modifier = Modifier.padding(vertical = 1.pad)
                    )
                }

                Spacer(modifier = Modifier.height(2.pad))

                if (showExpirationOptions) {
                    Menu(
                        onDismissRequest = { showExpirationOptions = false }
                    ) {
                        ExpirationOption.entries.forEach { option ->
                            menuItem(
                                title = stringResource(option.label),
                                action = {
                                    expiration = option
                                    showExpirationOptions = false
                                }
                            )
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val distanceValue = distance.toDoubleOrNull() ?: 10.0
                            val expirationInstant = Clock.System.now() + expiration.duration
                            onSave(name, distanceValue, expirationInstant)
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        )
    }
}
