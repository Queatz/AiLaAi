package com.queatz.ailaai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.connectivity
import com.queatz.ailaai.services.ui
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.scripts.PreviewScriptAction
import com.queatz.ailaai.ui.scripts.ScriptsDialog

@Composable
fun ProfileMenu(onDismissRequest: () -> Unit) {
    val me = me
    val nav = nav
    var showScriptsDialog by rememberStateOf(false)
    val hasConnectivity = connectivity.hasConnectivity

    if (showScriptsDialog) {
        ScriptsDialog(
            onDismissRequest = {
                showScriptsDialog = false
            },
            previewScriptAction = PreviewScriptAction.Edit
        )
    }

    Menu(onDismissRequest = onDismissRequest) {
        Toolbar {
            val showOfflineNote by ui.showOfflineNote.collectAsState()

            me?.let { me ->
                item(
                    icon = Icons.Outlined.Person,
                    name = stringResource(R.string.profile)
                ) {
                    nav.appNavigate(AppNav.Profile(me.id!!))
                    onDismissRequest()
                }
            }
            item(
                icon = Icons.Outlined.Rocket,
                name = stringResource(R.string.inventory),
            ) {
                nav.appNavigate(AppNav.Inventory)
                onDismissRequest()
            }
            item(
                icon = Icons.Outlined.HistoryEdu,
                name = stringResource(R.string.scripts),
            ) {
                showScriptsDialog = true
            }
            item(
                icon = if (hasConnectivity) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                name = if (!showOfflineNote) {
                    stringResource(R.string.show_offline_notes)
                } else {
                    stringResource(R.string.hide_offline_notes)
                },
            ) {
                ui.showOfflineNote(!showOfflineNote)
                onDismissRequest()
            }
            item(
                icon = Icons.Outlined.Settings,
                name = stringResource(R.string.settings),
            ) {
                nav.appNavigate(AppNav.Settings)
                onDismissRequest()
            }
        }
    }
}
