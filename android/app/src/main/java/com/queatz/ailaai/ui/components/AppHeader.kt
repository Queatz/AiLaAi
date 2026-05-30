package com.queatz.ailaai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.LocalAppState
import com.queatz.ailaai.R
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.connectivity
import com.queatz.ailaai.services.ui
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

private val offlineNoteKey = stringPreferencesKey("app.offlineNote")

@Composable
fun ColumnScope.AppHeader(
    title: String = "",
    onTitleClick: () -> Unit = {},
    showSignalsButton: Boolean = false,
    showProfile: Boolean = false,
    showOfflineNote: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    val context = LocalContext.current
    val me = me
    val hasConnectivity = connectivity.hasConnectivity
    val apiIsReachable = LocalAppState.current.apiIsReachable
    val showOfflineNotes by ui.showOfflineNote.collectAsState()
    val nav = nav

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (showOfflineNote) {

            var offlineNote by rememberStateOf("")
            var showClearDialog by rememberStateOf(false)

            LaunchedEffect(Unit) {
                offlineNote = context.dataStore.data.first()[offlineNoteKey].orEmpty()
            }

            LaunchedEffect(offlineNote) {
                delay(0.125.seconds)
                context.dataStore.edit {
                    it[offlineNoteKey] = offlineNote
                }
            }
            if (showClearDialog) {
                Alert(
                    onDismissRequest = {
                        showClearDialog = false
                    },
                    title = stringResource(R.string.discard_offline_note),
                    text = null,
                    dismissButton = stringResource(R.string.cancel),
                    confirmButton = stringResource(R.string.delete),
                    confirmColor = MaterialTheme.colorScheme.error
                ) {
                    offlineNote = ""
                    showClearDialog = false
                }
            }

            AnimatedVisibility(offlineNote.isNotEmpty() || !apiIsReachable || !hasConnectivity || showOfflineNotes) {
                SearchField(
                    value = offlineNote,
                    onValueChange = { offlineNote = it },
                    singleLine = false,
                    useMaxWidth = false,
                    useMaxHeight = true,
                    placeholder = stringResource(R.string.offline_notes),
                    icon = if (hasConnectivity) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                    onClear = {
                        showClearDialog = true
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            AppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.pad)
                    ) {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onTitleClick()
                                }
                        )
                    }
                },
                navigationIcon = navigationIcon,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = .5f.pad)
                    ) {
                        actions()
                        me?.takeIf { showProfile }?.let { me ->
                            var showProfileMenu by rememberStateOf(false)

                            if (showProfileMenu) {
                                ProfileMenu {
                                    showProfileMenu = false
                                }
                            }

                            if (me.name?.isNotBlank() == true || me.photo?.isNotBlank() == true) {
                                GroupPhoto(
                                    listOf(ContactPhoto(me.name ?: "", me.photo, me.seen)),
                                    size = 40.dp,
                                    modifier = Modifier
                                        .clickable {
                                            showProfileMenu = true
                                        }
                                )
                            } else {
                                IconButton({
                                    showProfileMenu = true
                                }) {
                                    Icon(Icons.Outlined.AccountCircle, Icons.Outlined.Settings.name)
                                }
                            }
                        }
                    }
                }
            )

            if (showSignalsButton) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 12.pad),
                    contentAlignment = Alignment.Center
                ) {
                    SignalsButton {
                        nav.appNavigate(AppNav.Signals)
                    }
                }
            }
        }
    }
}
