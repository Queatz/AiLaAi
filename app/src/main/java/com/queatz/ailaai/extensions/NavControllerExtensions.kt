package com.queatz.ailaai.extensions

import android.app.Activity
import androidx.navigation.NavController

fun NavController.popBackStackOrFinish() {
    if (!popBackStack()) {
        (context as? Activity)?.finish()
    }
}
