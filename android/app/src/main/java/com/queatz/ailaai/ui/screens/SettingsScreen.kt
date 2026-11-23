package com.queatz.ailaai.ui.screens

import account
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import app.ailaai.api.*
import coil3.imageLoader
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.AppUi
import com.queatz.ailaai.BuildConfig
import com.queatz.ailaai.R
import com.queatz.ailaai.appLanguage
import com.queatz.ailaai.cache.CacheMode
import com.queatz.ailaai.cache.CacheSizeLimit
import com.queatz.ailaai.cache.groupMessageCache
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.json
import com.queatz.ailaai.db.db
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.BiometricPrompt
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.InviteDialog
import com.queatz.ailaai.ui.dialogs.QrCodeDialog
import com.queatz.ailaai.ui.dialogs.ReleaseNotesDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.AppFeedback
import com.queatz.db.AppFeedbackType
import com.queatz.db.PersonProfile
import com.queatz.db.Profile
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import linkDevice

private sealed class PendingCacheDowngrade {
    abstract val bytesToDelete: Long?

    data class Mode(
        val targetMode: CacheMode,
        override val bytesToDelete: Long? = null,
    ) : PendingCacheDowngrade()

    data class SizeLimit(
        val targetLimit: CacheSizeLimit,
        override val bytesToDelete: Long? = null,
    ) : PendingCacheDowngrade()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appUi: AppUi,
    onAppUi: (AppUi) -> Unit,
    updateMe: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var signOutDialog by rememberStateOf(false)
    var exportDataDialog by rememberStateOf(false)
    var inviteDialog by rememberStateOf(false)
    var urlDialog by rememberStateOf(false)
    var showReleaseNotes by rememberStateOf(false)
    var showRefreshDialog by rememberStateOf(false)
    var transferCode by rememberStateOf("")
    var isRefreshing by rememberStateOf(false)
    var showBiometrics by rememberStateOf(false)
    var biometricsSucceeded by rememberStateOf(false)
    var onBiometricsSucceeded by rememberStateOf<(() -> Unit)?>(null)
    var showLinkDeviceDialog by rememberStateOf(false)
    var linkDeviceToken by rememberStateOf("")
    var profile by rememberSaveable(stateSaver = jsonSaver<PersonProfile?>()) {
        mutableStateOf(null)
    }
    var points by rememberStateOf<Int?>(null)
    val me = me
    val nav = nav
    val cacheMode by groupMessageCache.mode.collectAsState()
    val cacheSizeLimit by groupMessageCache.sizeLimit.collectAsState()
    var isUpdatingCacheSetting by rememberStateOf(false)
    var cacheModeMenuExpanded by rememberStateOf(false)
    var cacheSizeLimitMenuExpanded by rememberStateOf(false)
    var pendingCacheDowngrade by remember { mutableStateOf<PendingCacheDowngrade?>(null) }
    var totalCacheBytes by rememberStateOf<Long?>(null)

    suspend fun currentImageCacheBytes(): Long = withContext(Dispatchers.IO) {
        runCatching { context.imageLoader.diskCache?.size ?: 0L }.getOrDefault(0L)
    }

    suspend fun refreshCacheSize() {
        val messageBytes = groupMessageCache.sizeBytes()
        val imageBytes = currentImageCacheBytes()
        totalCacheBytes = messageBytes + imageBytes
    }

    fun loadTransferCode() {
        scope.launch {
            api.transferCode {
                transferCode = it.code!!
            }
        }
    }

    fun showTransferCode() {
        if (biometricsSucceeded) {
            loadTransferCode()
        } else {
            onBiometricsSucceeded = {
                loadTransferCode()
            }
            showBiometrics = true
        }
    }

    fun loadLinkDeviceToken() {
        if (biometricsSucceeded) {
            scope.launch {
                api.linkDevice {
                    linkDeviceToken = it.token ?: ""
                    showLinkDeviceDialog = true
                }
            }
        } else {
            onBiometricsSucceeded = {
                scope.launch {
                    api.linkDevice {
                        linkDeviceToken = it.token ?: ""
                        showLinkDeviceDialog = true
                    }
                }
            }
            showBiometrics = true
        }
    }

    BiometricPrompt(
        showBiometrics,
        onError = {
            showBiometrics = false

            if (it) {
                context.showDidntWork(R.string.error_verify_biometrics)
            } else {
                biometricsSucceeded = true
                onBiometricsSucceeded?.invoke()
            }
        },
        onFailed = {
            showBiometrics = false
        }
    ) {
        showBiometrics = false
        biometricsSucceeded = true
        onBiometricsSucceeded?.invoke()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val meId = me?.id

            if (meId == null) {
                delay(1_000)
                continue
            }

            api.profile(meId) {
                profile = it
            }
            break
        }
    }

    ResumeEffect {
        api.profile(me?.id ?: return@ResumeEffect) {
            profile = it
        }
    }

    ResumeEffect {
        api.account {
            points = it.points ?: 0
        }
    }

    ResumeEffect {
        refreshCacheSize()
    }

    if (inviteDialog) {
        InviteDialog(
            me?.name ?: context.getString(R.string.someone)
        ) { 
            inviteDialog = false
        }
    }

    if (urlDialog) {
        TextFieldDialog(
            onDismissRequest = { urlDialog = false },
            title = stringResource(R.string.your_profile_url),
            button = stringResource(R.string.update),
            singleLine = true,
            initialValue = profile?.profile?.url ?: "",
        ) { value ->
            api.updateProfile(
                Profile(url = value.trim()),
                onError = {
                    if (it.status == HttpStatusCode.Conflict) {
                        context.toast(R.string.url_already_in_use)
                    }
                }
            ) {
                urlDialog = false
                scope.launch {
                    api.profile(profile!!.profile.person!!) {
                        profile = it
                    }
                }
            }
        }
    }

    var isExportingData by rememberStateOf(false)

    fun exportData() {
        isExportingData = true
        scope.launch {
            api.exportData {
                json.encodeToString(it).shareAsTextFile(context, "data.json", ContentType.Text.Plain)
            }
            isExportingData = false
            exportDataDialog = false
        }
    }

    if (exportDataDialog) {
        AlertDialog({
            exportDataDialog = false
        },
            text = {
                Text(stringResource(R.string.data_export_description))
            },
            confirmButton = {
                Button(
                    {
                        exportData()
                    },
                    enabled = !isExportingData
                ) {
                    Text(stringResource(R.string.export_data))
                }
            })
    }

    if (signOutDialog) {
        var confirmSignOut by rememberStateOf(false)
        var confirmSignOutChecked by rememberStateOf(false)

        fun signOut() {
            if (biometricsSucceeded) {
                scope.launch {
                    api.signOut()
                    db.clear()
                    groupMessageCache.clear()
                    updateMe()
                    signOutDialog = false
                }
            } else {
                onBiometricsSucceeded = {
                    signOut()
                }
                showBiometrics = true
            }
        }

        AlertDialog(
            {
                signOutDialog = false
            },
            {
                TextButton(
                    {
                        signOutDialog = false
                    }
                ) {
                    Text(stringResource(R.string.go_back))
                }
                TextButton(
                    {
                        if (confirmSignOut) {
                            signOut()
                        } else {
                            confirmSignOut = true
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !confirmSignOut || confirmSignOutChecked
                ) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    if (confirmSignOut) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .clickable {
                                    confirmSignOutChecked = !confirmSignOutChecked
                                }) {
                            Checkbox(confirmSignOutChecked, {
                                confirmSignOutChecked = it
                            })
                            Text(stringResource(R.string.sign_out_confirmation))
                        }
                    } else if (transferCode.isNotBlank()) {
                        Text(stringResource(R.string.your_transfer_code_is))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                1.pad
                            )
                        ) {
                            SelectionContainer(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Text(
                                    transferCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.pad)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                        .padding(horizontal = 2.pad, vertical = 1.pad)
                                )
                            }
                            IconButton(
                                onClick = {
                                    showRefreshDialog = true
                                },
                                enabled = !isRefreshing
                            ) {
                                Icon(Icons.Outlined.Refresh, null)
                            }
                        }
                        Text(stringResource(R.string.sign_out_warning))
                    } else {
                        Text(stringResource(R.string.sign_out_description))
                        Button(
                            {
                                showTransferCode()
                            }
                        ) {
                            Text(stringResource(R.string.show_transfer_code))
                        }
                    }
                }
            }
        )
    }

    if (showRefreshDialog) {
        AlertDialog(
            {
                showRefreshDialog = false
            },
            title = {
                Text(stringResource(R.string.refresh_transfer_code))
            },
            text = {
                Text(stringResource(R.string.refresh_transfer_code_description))
            },
            dismissButton = {
                TextButton({
                    showRefreshDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton({
                    showRefreshDialog = false
                    isRefreshing = true
                    scope.launch {
                        api.refreshTransferCode {
                            transferCode = it.code!!
                        }
                        isRefreshing = false
                    }
                }) {
                    Text(stringResource(R.string.yes))
                }
            }
        )
    }

    if (showReleaseNotes) {
        ReleaseNotesDialog {
            showReleaseNotes = false
        }
    }

    if (showLinkDeviceDialog) {
        QrCodeDialog(
            onDismissRequest = { showLinkDeviceDialog = false },
            url = "$appDomain/link-device/$linkDeviceToken",
            name = stringResource(R.string.app_name),
            title = stringResource(R.string.link_device)
        )
    }

    Column {
        AppBar(
            title = {
                Text(
                    stringResource(R.string.settings),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                ElevatedButton(
                    onClick = {
                        inviteDialog = true
                    },
                    modifier = Modifier
                        .padding(horizontal = 2.pad)
                ) {
                    Icon(
                        Icons.Outlined.PersonAdd,
                        stringResource(R.string.invite_someone),
                        modifier = Modifier.padding(end = 1.pad)
                    )
                    Text(stringResource(R.string.invite_someone))
                }
            },
            navigationIcon = {
                BackButton()
            }
        )

        fun setLanguage(language: String) {
            scope.launch(Dispatchers.Main) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language)
                )
            }
        }

        var sendAppFeedback by rememberStateOf<AppFeedbackType?>(null)

        if (sendAppFeedback != null) {
            TextFieldDialog(
                {
                    sendAppFeedback = null
                },
                title = when (sendAppFeedback) {
                    AppFeedbackType.Suggestion -> stringResource(R.string.request_a_new_feature)
                    AppFeedbackType.Issue -> stringResource(R.string.report_a_bug)
                    AppFeedbackType.Other -> stringResource(R.string.app_feedback)
                    else -> ""
                },
                button = stringResource(R.string.send),
                requireNotBlank = true
            ) {
                api.sendAppFeedback(AppFeedback(feedback = it, type = sendAppFeedback!!)) {
                    sendAppFeedback = null
                    context.toast(R.string.thank_you)
                }
            }
        }

        var chooseLanguageDialog by rememberStateOf(false)
        var showPointsDialog by rememberStateOf(false)

        if (showPointsDialog) {
            Alert(
                onDismissRequest = {
                    showPointsDialog = false
                },
                title = stringResource(R.string.points),
                text = stringResource(R.string.points_description),
                dismissButton = null,
                confirmButton = stringResource(R.string.close),
                onConfirm = {
                    showPointsDialog = false
                }
            )
        }

        if (chooseLanguageDialog) {
            Dialog({
                chooseLanguageDialog = false
            }) {
                Surface(
                    shape = MaterialTheme.shapes.large
                ) {
                    Column {
                        DropdownMenuItem({ Text(stringResource(R.string.language_vietnamese)) }, {
                            setLanguage("vi,en")
                            chooseLanguageDialog = false
                        })
                        DropdownMenuItem({ Text(stringResource(R.string.language_english)) }, {
                            setLanguage("en,vi")
                            chooseLanguageDialog = false
                        })
                    }
                }
            }
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
        ) {
            DropdownMenuItem({
                Column(modifier = Modifier.padding(1.pad)) {
                    Text(
                        text = if (points == null) stringResource(R.string.loading_points) else {
                            pluralStringResource(R.plurals.x_points, points!!, points!!)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                    )
                    Text(
                        text = stringResource(R.string.account),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, {
                showPointsDialog = true
            })

            DropdownMenuItem({
                Column(modifier = Modifier.padding(1.pad)) {
                    Text(
                        stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                    )
                    Text(
                        when {
                            appLanguage?.startsWith("vi") == true -> stringResource(R.string.language_vietnamese)
                            appLanguage?.startsWith("zh") == true -> stringResource(R.string.language_chinese)
                            appLanguage?.startsWith("ru") == true -> stringResource(R.string.language_russian)
                            else -> stringResource(R.string.language_english)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, {
                chooseLanguageDialog = true
            })

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropdownMenuItem({
                    Column(modifier = Modifier.padding(1.pad)) {
                        Text(
                            stringResource(R.string.your_profile_url),
                            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                        )
                        Text(
                            profile?.profile?.url?.let { "$appDomain/$it" } ?: stringResource(R.string.none),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }, {
                    if (me != null) {
                        urlDialog = true
                    }
                }, modifier = Modifier.weight(1f))
                AnimatedVisibility(profile?.profile?.url?.isNotBlank() == true) {
                    IconButton(
                        {
                            "$appDomain/${profile!!.profile.url!!}".copyToClipboard(context)
                            context.toast(R.string.copied)
                        },
                        modifier = Modifier.padding(1.pad)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            null
                        )
                    }
                }
            }

            DropdownMenuItem({
                Text(
                    stringResource(R.string.link_device),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(1.pad)
                )
            }, {
                loadLinkDeviceToken()
            })

            Column(
                modifier = Modifier
                    .padding(horizontal = 2.pad, vertical = 1.pad)
                    .shadow(1.dp, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Text(
                    text = stringResource(R.string.cache),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 1.pad, start = 1.5f.pad, end = 1.5f.pad)
                )

                fun applyCacheMode(mode: CacheMode) {
                    if (isUpdatingCacheSetting || mode == cacheMode) {
                        return
                    }
                    scope.launch {
                        isUpdatingCacheSetting = true
                        groupMessageCache.setMode(mode)
                            .onFailure {
                                context.toast(R.string.error_save_cache_setting)
                            }
                        refreshCacheSize()
                        isUpdatingCacheSetting = false
                    }
                }

                fun applyCacheSizeLimit(limit: CacheSizeLimit) {
                    if (isUpdatingCacheSetting || limit == cacheSizeLimit) {
                        return
                    }
                    scope.launch {
                        isUpdatingCacheSetting = true
                        groupMessageCache.setSizeLimit(limit)
                            .onFailure {
                                context.toast(R.string.error_save_cache_setting)
                            }
                        refreshCacheSize()
                        isUpdatingCacheSetting = false
                    }
                }

                fun requestCacheModeChange(mode: CacheMode) {
                    if (isUpdatingCacheSetting || mode == cacheMode) {
                        return
                    }
                    if (mode.ordinal < cacheMode.ordinal) {
                        val initial = PendingCacheDowngrade.Mode(targetMode = mode)
                        pendingCacheDowngrade = initial
                        scope.launch {
                            val imageBytes = currentImageCacheBytes()
                            val bytesToDelete = groupMessageCache.estimateBytesDeletedOnModeChange(
                                targetMode = mode,
                                currentImageCacheBytes = imageBytes,
                            )
                            if (pendingCacheDowngrade == initial) {
                                pendingCacheDowngrade = initial.copy(bytesToDelete = bytesToDelete)
                            }
                        }
                    } else {
                        applyCacheMode(mode)
                    }
                }

                fun requestCacheSizeLimitChange(limit: CacheSizeLimit) {
                    if (isUpdatingCacheSetting || limit == cacheSizeLimit) {
                        return
                    }
                    if (limit.ordinal < cacheSizeLimit.ordinal) {
                        val initial = PendingCacheDowngrade.SizeLimit(targetLimit = limit)
                        pendingCacheDowngrade = initial
                        scope.launch {
                            val imageBytes = currentImageCacheBytes()
                            val bytesToDelete = groupMessageCache.estimateBytesDeletedOnSizeLimitChange(
                                targetLimit = limit,
                                currentImageCacheBytes = imageBytes,
                            )
                            if (pendingCacheDowngrade == initial) {
                                pendingCacheDowngrade = initial.copy(bytesToDelete = bytesToDelete)
                            }
                        }
                    } else {
                        applyCacheSizeLimit(limit)
                    }
                }

                @Composable
                fun cacheModeLabel(mode: CacheMode): String = when (mode) {
                    CacheMode.None -> stringResource(R.string.cache_mode_none)
                    CacheMode.Recent100 -> stringResource(R.string.cache_mode_recent_100)
                    CacheMode.All -> stringResource(R.string.cache_mode_all)
                }

                @Composable
                fun cacheSizeLimitLabel(limit: CacheSizeLimit): String = when (limit) {
                    CacheSizeLimit.Mb256 -> stringResource(R.string.cache_size_256mb)
                    CacheSizeLimit.Mb512 -> stringResource(R.string.cache_size_512mb)
                    CacheSizeLimit.Gb1 -> stringResource(R.string.cache_size_1gb)
                    CacheSizeLimit.Gb2 -> stringResource(R.string.cache_size_2gb)
                    CacheSizeLimit.Gb4 -> stringResource(R.string.cache_size_4gb)
                    CacheSizeLimit.Gb8 -> stringResource(R.string.cache_size_8gb)
                    CacheSizeLimit.Gb16 -> stringResource(R.string.cache_size_16gb)
                    CacheSizeLimit.Gb32 -> stringResource(R.string.cache_size_32gb)
                    CacheSizeLimit.Gb64 -> stringResource(R.string.cache_size_64gb)
                    CacheSizeLimit.Unlimited -> stringResource(R.string.cache_size_unlimited)
                }

                pendingCacheDowngrade?.let { pending ->
                    val bytesToDelete = pending.bytesToDelete
                    AlertDialog(
                        onDismissRequest = {
                            pendingCacheDowngrade = null
                        },
                        title = {
                            Text(stringResource(R.string.cache_downgrade_confirm_title))
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(1.pad)
                            ) {
                                Text(
                                    text = when (pending) {
                                        is PendingCacheDowngrade.Mode -> stringResource(
                                            R.string.cache_mode_downgrade_message,
                                            cacheModeLabel(pending.targetMode),
                                        )

                                        is PendingCacheDowngrade.SizeLimit -> stringResource(
                                            R.string.cache_size_limit_downgrade_message,
                                            cacheSizeLimitLabel(pending.targetLimit),
                                        )
                                    }
                                )
                                Text(
                                    text = when {
                                        bytesToDelete == null -> stringResource(R.string.loading_cache_items)
                                        bytesToDelete > 0L -> stringResource(
                                            R.string.cache_to_be_deleted_x,
                                            Formatter.formatShortFileSize(context, bytesToDelete),
                                        )

                                        else -> stringResource(R.string.cache_to_be_deleted_none)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if ((bytesToDelete ?: 0L) > 0L) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingCacheDowngrade = null
                                }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    pendingCacheDowngrade = null
                                    when (pending) {
                                        is PendingCacheDowngrade.Mode -> applyCacheMode(pending.targetMode)
                                        is PendingCacheDowngrade.SizeLimit -> applyCacheSizeLimit(pending.targetLimit)
                                    }
                                },
                                enabled = bytesToDelete != null && !isUpdatingCacheSetting,
                                colors = if ((bytesToDelete ?: 0L) > 0L) {
                                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                } else {
                                    ButtonDefaults.textButtonColors()
                                },
                            ) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = cacheModeMenuExpanded && !isUpdatingCacheSetting,
                    onExpandedChange = { expanded ->
                        if (!isUpdatingCacheSetting) {
                            cacheModeMenuExpanded = expanded
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 1.5f.pad, end = 1.5f.pad, top = 1.pad, bottom = .5f.pad)
                ) {
                    OutlinedTextField(
                        value = cacheModeLabel(cacheMode),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        enabled = !isUpdatingCacheSetting,
                        label = { Text(stringResource(R.string.cache_mode)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = cacheModeMenuExpanded && !isUpdatingCacheSetting
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = !isUpdatingCacheSetting,
                            )
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = cacheModeMenuExpanded && !isUpdatingCacheSetting,
                        onDismissRequest = { cacheModeMenuExpanded = false },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        CacheMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(cacheModeLabel(mode)) },
                                trailingIcon = if (mode == cacheMode) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                                onClick = {
                                    cacheModeMenuExpanded = false
                                    requestCacheModeChange(mode)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                val cacheSizeLimitEnabled = cacheMode != CacheMode.None && !isUpdatingCacheSetting

                ExposedDropdownMenuBox(
                    expanded = cacheSizeLimitMenuExpanded && cacheSizeLimitEnabled,
                    onExpandedChange = { expanded ->
                        if (cacheSizeLimitEnabled) {
                            cacheSizeLimitMenuExpanded = expanded
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 1.5f.pad, end = 1.5f.pad, top = .5f.pad, bottom = .5f.pad)
                ) {
                    OutlinedTextField(
                        value = cacheSizeLimitLabel(cacheSizeLimit),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        enabled = cacheSizeLimitEnabled,
                        label = { Text(stringResource(R.string.cache_size_limit)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = cacheSizeLimitMenuExpanded && cacheSizeLimitEnabled
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = cacheSizeLimitEnabled,
                            )
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = cacheSizeLimitMenuExpanded && cacheSizeLimitEnabled,
                        onDismissRequest = { cacheSizeLimitMenuExpanded = false },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        CacheSizeLimit.entries.forEach { limit ->
                            DropdownMenuItem(
                                text = { Text(cacheSizeLimitLabel(limit)) },
                                trailingIcon = if (limit == cacheSizeLimit) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                                onClick = {
                                    cacheSizeLimitMenuExpanded = false
                                    requestCacheSizeLimitChange(limit)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = .5f.pad)) {
                            Text(
                                text = stringResource(R.string.clear_cache),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            totalCacheBytes?.let { bytes ->
                                Text(
                                    text = if (bytes > 0L) {
                                        Formatter.formatShortFileSize(context, bytes)
                                    } else {
                                        stringResource(R.string.clear_cache_empty)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    },
                    onClick = {
                        nav.appNavigate(AppNav.ClearCache)
                    },
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 2.pad, vertical = 1.pad)
                    .shadow(1.dp, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
//                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Text(
                    stringResource(R.string.improve_the_app),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 1.pad, start = 1.5f.pad, end = 1.5f.pad)
                )

                DropdownMenuItem({
                    Text(
                        stringResource(R.string.request_a_new_feature),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }, {
                    sendAppFeedback = AppFeedbackType.Suggestion
                })

                DropdownMenuItem({
                    Text(
                        stringResource(R.string.report_a_bug),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }, {
                    sendAppFeedback = AppFeedbackType.Issue
                })

                DropdownMenuItem({
                    Text(
                        stringResource(R.string.app_feedback),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }, {
                    sendAppFeedback = AppFeedbackType.Other
                })
            }

            DropdownMenuItem({
                Column(
                    modifier = Modifier.padding(1.pad)
                ) {
                    Text(
                        stringResource(R.string.release_history),
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                    )
                    Text(
                        stringResource(R.string.app_version_x, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, onClick = {
                showReleaseNotes = true
            })

            DropdownMenuItem({
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${stringResource(R.string.app_name)} ${stringResource(R.string.website)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(1.pad)
                    )

                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }, {
                appDomain.launchUrl(context)
            })

            DropdownMenuItem({
                Text(
                    if (appUi.showNavLabels) {
                        stringResource(R.string.hide_navigation_labels)
                    } else {
                        stringResource(R.string.show_navigation_labels)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(1.pad)
                )
            }, {
                onAppUi(appUi.copy(showNavLabels = !appUi.showNavLabels))
            })

            DropdownMenuItem({
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.privacy_policy),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(1.pad)
                    )

                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }, {
                "$appDomain/privacy".launchUrl(context)
            })

            DropdownMenuItem({
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.terms_of_use),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(1.pad)
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }, {
                "$appDomain/terms".launchUrl(context)
            })

            DropdownMenuItem({
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.open_source),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(1.pad)
                    )

                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }, {
                "$appDomain/info/open-source".launchUrl(context)
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.export_data),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(1.pad)
                )
            }, {
                exportDataDialog = true
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.sign_out_or_transfer),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(1.pad)
                )
            }, {
                signOutDialog = true
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.made_with_love),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(1.pad)
                )
            }, {
            }, enabled = false)
        }
    }
}
