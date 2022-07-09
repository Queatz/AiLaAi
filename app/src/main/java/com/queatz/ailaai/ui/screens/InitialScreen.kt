package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.client.plugins.*
import kotlinx.coroutines.launch

@Composable
fun InitialScreen(onKnown: () -> Unit) {
    var codeValue by remember { mutableStateOf("") }
    var codeValueEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun signUp(code: String) {
        coroutineScope.launch {
            try {
                val token = api.signUp(code).token
                api.setToken(token)
                onKnown()
            } catch (ex: Exception) {
                if (ex !is ResponseException) {
                    ex.printStackTrace()
                }
            } finally {
                codeValueEnabled = true
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(PaddingDefault * 4, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(
                PaddingValues(
                    top = PaddingDefault,
                    start = PaddingDefault,
                    end = PaddingDefault,
                    bottom = PaddingDefault * 8
                )
            )
    ) {
        Text(
            stringResource(R.string.hello),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(
            codeValue,
            onValueChange = {
                codeValue = it.take(6)

                if (it.length == 6) {
                    codeValueEnabled = false
                    signUp(it)
                }
            },
            enabled = codeValueEnabled,
            label = { Text(stringResource(R.string.enter_invite_code)) },
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                codeValueEnabled = false
                signUp(codeValue)
            }),
        )
        TextButton(
            {
            // todo
        }
        ) {
            Text(stringResource(R.string.enter_recovery_code))
        }
    }
}
