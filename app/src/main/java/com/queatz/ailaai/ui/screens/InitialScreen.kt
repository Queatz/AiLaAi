package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.signIn
import com.queatz.ailaai.api.signUp
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InitialScreen(onKnown: () -> Unit) {
    var codeValue by remember { mutableStateOf("") }
    var codeExpired by rememberStateOf(false)
    var codeValueEnabled by rememberStateOf(true)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current!!

    var signInDialog by rememberStateOf(false)

    fun signIn(transferCode: String) {
        scope.launch {
            api.signIn(transferCode) {
                api.setToken(it.token)
                onKnown()
                keyboardController.hide()
            }
        }
    }

    if (signInDialog) {
        AlertDialog(
            {
                signInDialog = false
            },
            {
                TextButton(
                    {
                        signInDialog = false
                    }
                ) {
                    Text(stringResource(R.string.go_back))
                }
            },
            title = {
                Text(stringResource(R.string.enter_transfer_code))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    var transferCode by remember { mutableStateOf("") }
                    OutlinedTextField(
                        transferCode,
                        {
                            transferCode = it.take(16)

                            if (transferCode.length == 16) {
                                signIn(transferCode)
                            }
                        },
                        placeholder = { Text(stringResource(R.string.transfer_code)) },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                    )
                }
            }
        )
    }

    fun signUp(code: String? = null) {
        scope.launch {
            api.signUp(code, onError = { ex ->
                if (ex is ResponseException) {
                    if (ex.response.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.NotFound)) {
                        codeExpired = true
                    }
                }
            }) {
                api.setToken(it.token)
                onKnown()
                keyboardController.hide()
            }
            codeValueEnabled = true
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterVertically),
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

                if (codeExpired) {
                    codeExpired = false
                }

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

        if (codeExpired) {
            Text(
                stringResource(R.string.code_expired),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(PaddingDefault * 2)) {
            TextButton(
                {
                    signUp()
                },
                modifier = Modifier.padding(vertical = PaddingDefault * 3)
            ) {
                Text(stringResource(R.string.sign_up))
            }
            TextButton(
                {
                    signInDialog = true
                },
                modifier = Modifier.padding(vertical = PaddingDefault * 3)
            ) {
                Text(stringResource(R.string.sign_in))
            }
        }
    }
}
