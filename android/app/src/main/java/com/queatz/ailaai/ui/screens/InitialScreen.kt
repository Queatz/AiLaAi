package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ailaai.api.signIn
import app.ailaai.api.signInWithLink
import app.ailaai.api.signUp
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.helpers.UserSession
import com.queatz.ailaai.extensions.didntWorkMessage
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.components.ScanQrCodeResult
import com.queatz.ailaai.ui.theme.pad
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun InitialScreen(onKnown: (isSignUp: Boolean) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var codeValue by remember { mutableStateOf("") }
    var codeExpired by rememberStateOf(false)
    var codeValueEnabled by rememberStateOf(true)
    var isSigningUp by rememberStateOf(false)
    var signUpError by rememberStateOf<String?>(null)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var signInDialog by rememberStateOf(false)
    var transferCode by remember { mutableStateOf("") }
    var isSigningIn by rememberStateOf(false)
    var signInError by rememberStateOf<String?>(null)
    val isBusy = isSigningUp || isSigningIn

    suspend fun formatError(t: Throwable): String {
        return if (t is ResponseException) {
            val status = t.response.status
            val body = runCatching { t.response.bodyAsText() }
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.startsWith("<") }
                ?.take(200)
            if (body != null) {
                resources.getString(R.string.server_error_x, status.value, body)
            } else {
                resources.getString(R.string.server_error_x, status.value, status.description)
            }
        } else {
            val message = t.localizedMessage ?: t.message
            val type = t::class.simpleName ?: resources.getString(R.string.error)
            if (!message.isNullOrBlank()) "$type: $message" else type
        }
    }

    fun signIn(code: String) {
        if (isBusy) return
        isSigningIn = true
        signInError = null
        scope.launch {
            try {
                api.signIn(
                    code,
                    onError = { ex ->
                        if (ex is CancellationException) throw ex
                        signInError = formatError(ex)
                        context.showDidntWork(R.string.error_sign_in)
                    }
                ) {
                    signInDialog = false
                    transferCode = ""
                    UserSession.clear(context)
                    api.setToken(it.token)
                    onKnown(false)
                    keyboardController?.hide()
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                signInError = formatError(t)
                context.showDidntWork(R.string.error_sign_in)
            } finally {
                isSigningIn = false
            }
        }
    }

    if (signInDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSigningIn) {
                    signInDialog = false
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        signIn(transferCode.trim())
                    },
                    enabled = transferCode.isNotBlank() && !isBusy
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(end = 1.pad)
                                .size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(stringResource(R.string.sign_in))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        signInDialog = false
                    },
                    enabled = !isSigningIn
                ) {
                    Text(stringResource(R.string.go_back))
                }
            },
            title = {
                Text(stringResource(R.string.enter_transfer_code))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    OutlinedTextField(
                        transferCode,
                        {
                            transferCode = it.take(16)
                            if (signInError != null) {
                                signInError = null
                            }

                            if (transferCode.length == 16 && !isBusy) {
                                signIn(transferCode)
                            }
                        },
                        enabled = !isBusy,
                        placeholder = { Text(stringResource(R.string.transfer_code)) },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (transferCode.isNotBlank() && !isBusy) {
                                signIn(transferCode.trim())
                            }
                        })
                    )

                    if (signInError != null) {
                        Text(
                            signInError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        )
    }

    fun signInWithLink(link: String) {
        if (isBusy) return
        isSigningIn = true
        signInError = null
        scope.launch {
            try {
                api.signInWithLink(
                    link = link,
                    onError = { ex ->
                        if (ex is CancellationException) throw ex
                        signInError = formatError(ex)
                        context.showDidntWork(R.string.error_sign_in)
                    }
                ) {
                    UserSession.clear(context)
                    api.setToken(it.token)
                    onKnown(false)
                    keyboardController?.hide()
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                signInError = formatError(t)
                context.showDidntWork(R.string.error_sign_in)
            } finally {
                isSigningIn = false
            }
        }
    }

    fun signUp(code: String? = null) {
        if (isBusy) return
        isSigningUp = true
        signUpError = null
        codeExpired = false
        codeValueEnabled = false
        scope.launch {
            try {
                api.signUp(
                    inviteCode = code,
                    onError = { ex ->
                        if (ex is CancellationException) throw ex
                        if (!code.isNullOrBlank() && ex is ResponseException && ex.response.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.NotFound)) {
                            codeExpired = true
                        } else {
                            signUpError = formatError(ex)
                            context.showDidntWork(R.string.error_sign_up)
                        }
                    }
                ) {
                    UserSession.clear(context)
                    api.setToken(it.token)
                    onKnown(true)
                    keyboardController?.hide()
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                signUpError = formatError(t)
                context.showDidntWork(R.string.error_sign_up)
            } finally {
                isSigningUp = false
                codeValueEnabled = true
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .background(
                linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    top = 1.pad,
                    start = 1.pad,
                    end = 1.pad,
                    bottom = 8.pad
                )
            )
    ) {
        Image(
            painterResource(R.mipmap.ic_app),
            null,
            modifier = Modifier
                .padding(bottom = 1.pad)
        )
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
                if (signUpError != null) {
                    signUpError = null
                }

                if (it.length == 6 && !isBusy) {
                    signUp(it)
                }
            },
            enabled = codeValueEnabled && !isBusy,
            label = { Text(stringResource(R.string.enter_invite_code)) },
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (!isBusy) {
                    signUp(codeValue.trim().ifBlank { null })
                }
            }),
        )

        if (codeExpired) {
            Text(
                stringResource(R.string.code_expired),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(2.pad),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 2.pad, vertical = 2.pad)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Button(
                    {
                        signUp(codeValue.trim().ifBlank { null })
                    },
                    enabled = !isBusy,
                    modifier = Modifier.padding(top = 1.pad)
                ) {
                    if (isSigningUp) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(end = 1.pad)
                                .size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(stringResource(R.string.sign_up))
                }

                if (signUpError != null) {
                    Text(
                        text = signUpError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.pad)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    {
                        signInDialog = true
                    },
                    enabled = !isBusy,
                    modifier = Modifier.padding(top = 1.pad)
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 1.pad)
                                .size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(stringResource(R.string.sign_in))
                }

                if (signInError != null) {
                    Text(
                        text = signInError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.pad)
                    )
                }
            }
        }

        ScanQrCodeButton(
            button = { scan ->
                OutlinedButton(
                    onClick = scan,
                    enabled = !isBusy,
                    modifier = Modifier.padding(vertical = 1.pad)
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_qr),
                        modifier = Modifier.padding(end = 1.pad)
                    )
                    Text(stringResource(R.string.scan_qr))
                }
            }
        ) { result ->
            // Handle standard app URLs
            when (result) {
                is ScanQrCodeResult.Invite -> {
                    signUp(result.code)
                }

                is ScanQrCodeResult.LinkDevice -> {
                    if (result.token.isNotBlank()) {
                        signInWithLink(result.token)
                    } else {
                        signInError = context.didntWorkMessage(R.string.error_invalid_link_code)
                    }
                }

                else -> Unit
            }
        }
    }
}
