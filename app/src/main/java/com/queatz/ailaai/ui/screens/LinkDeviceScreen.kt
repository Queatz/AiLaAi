package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.queatz.ailaai.api.confirmLinkDeviceToken
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.popBackStackOrFinish
import kotlinx.coroutines.launch

@Composable
fun LinkDeviceScreen(navController: NavController, token: String, me: () -> Person?) {
    val scope = rememberCoroutineScope()

    Column {
        Text("Are your sure you want to link this device?")
        Button({
            navController.popBackStackOrFinish()
        }) {
            Text("No")
        }
        OutlinedButton({
            scope.launch {
                api.confirmLinkDeviceToken(token) {
                    navController.popBackStackOrFinish()
                }
            }
        }) {
            Text("Yes")
        }
    }
}
