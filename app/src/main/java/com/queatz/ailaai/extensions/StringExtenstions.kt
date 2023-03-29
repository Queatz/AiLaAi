package com.queatz.ailaai.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import io.ktor.http.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

fun String.shareAsUrl(context: Context, name: String?) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_TEXT, this)
    intent.type = ContentType.Text.Plain.toString()
    if (name != null) {
        intent.putExtra(Intent.EXTRA_TITLE, name)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

// Returns true if everything went well
fun Bitmap.share(context: Context, name: String?): Boolean {
    val uri = uri(context) ?: return false
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.type = ContentType.Image.PNG.toString()
    if (name != null) {
        intent.putExtra(Intent.EXTRA_TITLE, name)
    }
    intent.setDataAndType(uri, context.contentResolver.getType(uri))
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.startActivity(Intent.createChooser(intent, null))
    return true
}

fun Bitmap.uri(context: Context): Uri? {
    try {
        val cachePath = File(context.cacheDir, "share")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/share.png")
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    val imagePath = File(context.cacheDir, "share")
    val newFile = File(imagePath, "share.png")
    return FileProvider.getUriForFile(context, "app.ailaai.share.fileprovider", newFile)

}
