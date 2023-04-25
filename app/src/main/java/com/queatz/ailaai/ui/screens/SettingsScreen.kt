package com.queatz.ailaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.appLanguage
import com.queatz.ailaai.extensions.sendEmail
import com.queatz.ailaai.ui.dialogs.InviteDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, me: () -> Person?, updateMe: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var signOutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var inviteDialog by remember { mutableStateOf(false) }

    if (inviteDialog) {
        InviteDialog(
            me()?.name ?: context.getString(R.string.someone)
        ) { inviteDialog = false }
    }

    if (signOutDialog) {
        var transferCode by remember { mutableStateOf("") }
        var confirmSignOut by remember { mutableStateOf(false) }
        var confirmSignOutChecked by remember { mutableStateOf(false) }

        fun signOut() {
            coroutineScope.launch {
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
                        SelectionContainer {
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
                        Text(stringResource(R.string.sign_out_warning))
                    } else {
                        Text(stringResource(R.string.sign_out_description))
                        Button(
                            {
                                coroutineScope.launch {
                                    try {
                                        transferCode = api.transferCode().code!!
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.show_transfer_code))
                        }
                    }
                }
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            try {
                api.updateMyPhoto(it)
                updateMe()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    Column {
        TopAppBar(
            {
                Text(stringResource(R.string.settings), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Icon(Icons.Outlined.PersonAdd, stringResource(R.string.invite), modifier = Modifier.padding(end = PaddingDefault))
                    Text(stringResource(R.string.invite))
                }
            },
            navigationIcon = {
                IconButton({
                    navController.popBackStack()
                }) {
                    Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                }
            }
        )

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
        ) {
            var chooseLanguageDialog by remember { mutableStateOf(false) }

            if (chooseLanguageDialog) {
                fun setLanguage(language: String) {
                    coroutineScope.launch(Dispatchers.Main) {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                }

                Dialog({
                    chooseLanguageDialog = false
                }) {
                    Surface(
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            DropdownMenuItem({ Text("Tiếng Việt") }, {
                                chooseLanguageDialog = false
                                setLanguage("vi,en")
                            })
                            DropdownMenuItem({ Text("English") }, {
                                chooseLanguageDialog = false
                                setLanguage("en,vi")
                            })
                        }
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
                        when (appLanguage) {
                            "vi" -> stringResource(R.string.language_vietnamese)
                            else -> stringResource(R.string.language_english)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, {
                chooseLanguageDialog = true
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.app_feedback),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                "Jacob<jacobaferrero@gmail.com>".sendEmail(context, "Ai Là Ai feedback")
            })

            DropdownMenuItem({
                Text(
                    stringResource(R.string.sign_out_or_transfer),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(PaddingDefault)
                )
            }, {
                signOutDialog = true
            })
        }
    }
}
