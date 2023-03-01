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

fun String.sendEmail(context: Context, subject: String? = null) {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(this))
    if (subject != null) {
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
