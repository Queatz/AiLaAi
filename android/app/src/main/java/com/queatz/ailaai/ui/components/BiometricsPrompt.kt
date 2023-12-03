package com.queatz.ailaai.ui.components

import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.queatz.ailaai.R

// https://github.com/yogeshpaliyal/KeyPass/blob/71d944bd6b63063615a2c49a95f7cf1db2babd4c/app/src/main/java/com/yogeshpaliyal/keypass/ui/auth/components/BiometricPrompt.kt#L19

@Composable
fun BiometricPrompt(
    show: Boolean,
    onError: (isBiometricsSupported: Boolean) -> Unit = {},
    onFailed: () -> Unit = {},
    onSuccess: () -> Unit
) {
    if (!show) {
        return
    }

    val context = LocalContext.current
    val title = stringResource(R.string.transfer_code)

    LaunchedEffect(context) {
        val fragmentActivity = context as? FragmentActivity ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    onError(
                        when (errorCode) {
                            ERROR_NO_BIOMETRICS,
                            ERROR_HW_NOT_PRESENT,
                            ERROR_NO_DEVICE_CREDENTIAL -> false
                            else -> true
                        }
                    )
                }

                override fun onAuthenticationSucceeded(
                    result: AuthenticationResult
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
