package com.queatz.ailaai.ui.dialogs


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.updateProfile
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Profile
import com.queatz.db.ProfileConfig
import kotlinx.coroutines.launch

@Composable
fun ProfileSettingsDialog(
    onDismissRequest: () -> Unit,
    profile: Profile?,
) {
    val profileConfig by remember(profile) { mutableStateOf(profile?.config ?: ProfileConfig()) }
    val recomposeScope = currentRecomposeScope
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)
    var isModified by rememberStateOf(false)

    fun modified() {
        recomposeScope.invalidate()
        isModified = true
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = true,
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                           .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.profile_setting),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                               .padding(bottom = 1.pad)
                        )
                    }
                    Check(
                        checked = profileConfig.showGroups ?: true,
                        onCheckChanged = {
                            profileConfig.showGroups =  it
                            modified()
                        },
                        label = {
                            Text(stringResource(R.string.show_open_groups))
                        }
                    )
                }
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                Button(
                    {
                        isLoading = true
                        scope.launch {
                            api.updateProfile(Profile(config = profileConfig)) {
                                onDismissRequest()
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && isModified
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        )
    }
}