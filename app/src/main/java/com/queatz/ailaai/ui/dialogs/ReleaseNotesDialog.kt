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
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.data.VersionInfo
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.launchUrl
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.PaddingDefault

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
                .padding(PaddingDefault * 3)
        ) {
            Text(
                stringResource(R.string.release_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            AnimatedVisibility(versionInfo != null) {
                versionInfo?.let { versionInfo ->
                    Text(
                        if (updateAvailable) {
                            stringResource(R.string.version_x_available, BuildConfig.VERSION_NAME, versionInfo.versionName)
                        } else {
                            stringResource(R.string.you_are_using_the_latest_version)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = PaddingDefault)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(releaseNotes)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
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
