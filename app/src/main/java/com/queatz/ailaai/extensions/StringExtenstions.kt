package com.queatz.ailaai.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri

fun String.launchUrl(context: Context) {
    context.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(this)
        )
    )
}

val String.nullIfBlank get() = takeIf { it.isNotBlank() }
