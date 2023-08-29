package com.queatz.ailaai.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.queatz.ailaai.BuildConfig
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.appLanguage
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.BiometricPrompt
import com.queatz.ailaai.ui.dialogs.InviteDialog
import com.queatz.ailaai.ui.dialogs.ReleaseNotesDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, me: () -> Person?, updateMe: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var signOutDialog by rememberStateOf(false)
    var exportDataDialog by rememberStateOf(false)
    var inviteDialog by rememberStateOf(false)
    var urlDialog by rememberStateOf(false)
    var showReleaseNotes by rememberStateOf(false)
    var showRefreshDialog by rememberStateOf(false)
    var profile by remember { mutableStateOf<PersonProfile?>(null) }
    var transferCode by remember { mutableStateOf("") }
    var isRefreshing by rememberStateOf(false)
    var showBiometrics by rememberStateOf(false)
    var biometricsSucceeded by rememberStateOf(false)

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
            showBiometrics = true
        }
    }

    BiometricPrompt(
        showBiometrics,
        onError = {
            showBiometrics = false
            context.showDidntWork()
        },
        onFailed = {
            showBiometrics = false
        }
    ) {
        showBiometrics = false
        biometricsSucceeded = true
        loadTransferCode()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val meId = me()?.id

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

    if (inviteDialog) {
        InviteDialog(
            me()?.name ?: context.getString(R.string.someone)
        ) { inviteDialog = false }
    }

    if (urlDialog) {
        TextFieldDialog(
            { urlDialog = false },
            stringResource(R.string.your_profile_url),
            stringResource(R.string.update),
            true,
            profile?.profile?.url ?: "",
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
            scope.launch {
                api.setToken(null)
                updateMe()
                signOutDialog = false
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
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    if (confirmSignOut) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(
                            PaddingDefault)) {
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
                                        .padding(vertical = PaddingDefault)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                        .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault)
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
                    Text(stringResource(R.string.continue_conversation))
                }
            }
        )
    }

    if (showReleaseNotes) {
        ReleaseNotesDialog {
            showReleaseNotes = false
        }
    }

    Column {
        TopAppBar(
            {
                Text(
                    stringResource(R.string.settings),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                ElevatedButton(
                    {
                        inviteDialog = true
                    },
                    enabled = !inviteDialog,
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault * 2)
                ) {
                    Icon(
                        Icons.Outlined.PersonAdd,
                        stringResource(R.string.invite_someone),
                        modifier = Modifier.padding(end = PaddingDefault)
                    )
                    Text(stringResource(R.string.invite_someone))
                }
            },
            navigationIcon = {
                BackButton(navController)
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropdownMenuItem({
                    Column(modifier = Modifier.padding(PaddingDefault)) {
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
                    if (me() != null) {
                        urlDialog = true
                    }
                }, modifier = Modifier.weight(1f))
                AnimatedVisibility(profile?.profile?.url?.isNotBlank() == true) {
                    IconButton(
                        {
                            "$appDomain/${profile!!.profile.url!!}".copyToClipboard(context)
                            context.toast(R.string.copied)
                        },
                        modifier = Modifier.padding(PaddingDefault)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            null
                        )
                    }
                }
            }

            DropdownMenuItem({
                Column(modifier = Modifier.padding(PaddingDefault)) {
                    Text(
                        stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                    )
                    Text(
                        when {
                            appLanguage?.startsWith("vi") == true -> stringResource(R.string.language_vietnamese)
                            appLanguage?.startsWith("zh") == true -> stringResource(R.string.language_chinese)
                            else -> stringResource(R.string.language_english)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, {
                chooseLanguageDialog = true
            })

            Column(
                modifier = Modifier
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault)
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
                    modifier = Modifier.padding(top = PaddingDefault, start = PaddingDefault * 1.5f, end = PaddingDefault * 1.5f)
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
                    modifier = Modifier.padding(PaddingDefault)
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
                Text(
                    stringResource(R.string.privacy_policy),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                "$appDomain/privacy".launchUrl(context)
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.terms_of_use),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                "$appDomain/page/open-source".launchUrl(context)
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.open_source),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                "$appDomain/terms".launchUrl(context)
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.export_data),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                exportDataDialog = true
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.sign_out_or_transfer),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                signOutDialog = true
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.made_with_love),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
            }, enabled = false)
        }
    }
}
