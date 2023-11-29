package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.confirmLinkDeviceToken
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch

@Composable
fun LinkDeviceScreen(token: String) {
    val scope = rememberCoroutineScope()
    val nav = nav

    LaunchedEffect(Unit) {
        // todo check if code is still valid
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(1.pad)
    ) {
        // todo translate
        Text(
            "Are your sure you want to link this device?",
            modifier = Modifier.padding(2.pad)
        )
        Button({
            nav.popBackStackOrFinish()
        }) {
            Text(stringResource(R.string.no))
        }
        OutlinedButton({
            scope.launch {
                api.confirmLinkDeviceToken(token) {
                    nav.popBackStackOrFinish()
                }
            }
        }) {
            Text(stringResource(R.string.yes))
        }
    }
}
