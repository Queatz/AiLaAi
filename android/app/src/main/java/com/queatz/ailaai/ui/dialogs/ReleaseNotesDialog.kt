package com.queatz.ailaai.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.BuildConfig
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.launchUrl
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.VersionInfo
import kotlinx.coroutines.delay

@Composable
fun ReleaseNotesDialog(onDismissRequest: () -> Unit) {
    var releaseNotes by rememberStateOf("")
    var versionInfo by rememberStateOf<VersionInfo?>(null)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            releaseNotes = api.appReleaseNotes()
        } catch (e: Exception) {
            e.printStackTrace()
            releaseNotes = context.getString(R.string.didnt_work)
        }
    }

    LaunchedEffect(Unit) {
        try {
            versionInfo = api.latestAppVersionInfo()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val updateAvailable = versionInfo?.let { it.versionCode > BuildConfig.VERSION_CODE } == true

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            Text(
                stringResource(R.string.release_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 1.pad)
            )
            AnimatedVisibility(versionInfo != null) {
                versionInfo?.let { versionInfo ->
                    Text(
                        if (updateAvailable) {
                            stringResource(R.string.version_x_available, versionInfo.versionName, BuildConfig.VERSION_NAME)
                        } else {
                            stringResource(R.string.you_are_using_the_latest_version)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 1.pad)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (releaseNotes.isEmpty()) {
                    Loading()
                } else {
                    Text(releaseNotes)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogCloseButton(onDismissRequest)
                AnimatedVisibility(updateAvailable) {
                    TextButton(
                        {
                            "$appDomain/ailaai-${versionInfo!!.versionName}.apk".launchUrl(context)
                        }
                    ) {
                        Text(stringResource(R.string.download))
                    }
                }
            }
        }
    }
}
