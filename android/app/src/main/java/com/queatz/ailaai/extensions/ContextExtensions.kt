package com.queatz.ailaai.extensions

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.queatz.ailaai.R

@Composable
fun AppCompatActivity.rememberIsInPipMode(): Boolean {
    var pipMode by remember { mutableStateOf(isInPictureInPictureMode) }
    DisposableEffect(this) {
        val observer = Consumer<PictureInPictureModeChangedInfo> { pipMode = it.isInPictureInPictureMode }
        addOnPictureInPictureModeChangedListener(observer)
        onDispose {
            removeOnPictureInPictureModeChangedListener(observer)
        }
    }
    return pipMode
}

fun Context.showDidntWork() = toast(getString(R.string.didnt_work))

fun Context.toast(@StringRes stringRes: Int) = toast(getString(stringRes))

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) = Toast
    .makeText(this, text, duration)
    .show()

val Context.isInstalledFromPlayStore: Boolean get() {
    val playStoreInstallers = listOf("com.android.vending", "com.google.android.feedback")

    // The package name of the app that has installed your app
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        packageManager.getInstallSourceInfo(packageName).initiatingPackageName
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstallerPackageName(packageName)
    }

    return installer != null && playStoreInstallers.contains(installer)
}
