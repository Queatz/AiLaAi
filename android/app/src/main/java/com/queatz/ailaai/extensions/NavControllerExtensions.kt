package com.queatz.ailaai.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.navigation.NavController

fun NavController.popBackStackOrFinish() {
    if (!popBackStack()) {
        (context as? Activity)?.finish()
    }
}

fun NavController.goToSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
    (context as Activity).startActivity(intent)
}
