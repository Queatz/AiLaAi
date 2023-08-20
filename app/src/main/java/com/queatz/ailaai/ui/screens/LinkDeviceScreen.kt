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
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.confirmLinkDeviceToken
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun LinkDeviceScreen(navController: NavController, token: String, me: () -> Person?) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // todo check if code is still valid
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingDefault)
    ) {
        // todo translate
        Text(
            "Are your sure you want to link this device?",
            modifier = Modifier.padding(PaddingDefault * 2)
        )
        Button({
            navController.popBackStackOrFinish()
        }) {
            Text(stringResource(R.string.no))
        }
        OutlinedButton({
            scope.launch {
                api.confirmLinkDeviceToken(token) {
                    navController.popBackStackOrFinish()
                }
            }
        }) {
            Text(stringResource(R.string.yes))
        }
    }
}
