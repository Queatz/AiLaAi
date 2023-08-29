package com.queatz.ailaai.ui.components

import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.queatz.ailaai.R

// https://github.com/yogeshpaliyal/KeyPass/blob/71d944bd6b63063615a2c49a95f7cf1db2babd4c/app/src/main/java/com/yogeshpaliyal/keypass/ui/auth/components/BiometricPrompt.kt#L19

@Composable
fun BiometricPrompt(show: Boolean, onError: () -> Unit = {}, onFailed: () -> Unit = {}, onSuccess: () -> Unit) {
    if (!show) {
        return
    }

    val context = LocalContext.current
    val title = stringResource(R.string.transfer_code)
    val cancel = stringResource(R.string.cancel)

    LaunchedEffect(context) {
        val fragmentActivity = context as? FragmentActivity ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    onError()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        )

        biometricPrompt.authenticate(
            PromptInfo.Builder()
                .setTitle(title)
                .setConfirmationRequired(false)
                .setAllowedAuthenticators(Authenticators.DEVICE_CREDENTIAL or Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }
}
