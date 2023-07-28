package com.queatz.ailaai.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.queatz.ailaai.data.api
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun String.wordCount() = if (isBlank()) 0 else trim().split("\\W+".toRegex()).size

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

fun String.shareAsText(context: Context) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_TEXT, this)
    intent.type = ContentType.Text.Plain.toString()
    context.startActivity(Intent.createChooser(intent, null))
}

fun String.copyToClipboard(context: Context, label: String? = null) {
    ContextCompat.getSystemService(context, ClipboardManager::class.java)?.setPrimaryClip(
        ClipData.newPlainText(label, this)
    )
}

private fun shareFile(uri: Uri, context: Context, name: String?, contentType: ContentType) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.type = contentType.toString()
    if (name != null) {
        intent.putExtra(Intent.EXTRA_TITLE, name)
    }
    intent.setDataAndType(uri, context.contentResolver.getType(uri))
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.startActivity(Intent.createChooser(intent, null))
}

// Returns true if everything went well
suspend fun Bitmap.share(context: Context, name: String?): Boolean {
    shareFile(uri(context) ?: return false, context, name, ContentType.Image.JPEG)
    return true
}

suspend fun Bitmap.uri(context: Context): Uri? {
    val path = File(context.cacheDir, "share")
    path.mkdirs()
    withContext(Dispatchers.IO) {
        val stream = FileOutputStream("$path/share.jpg")
        try {
            compress(Bitmap.CompressFormat.JPEG, 100, stream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            stream.close()
        }
    } ?: return null

    val newFile = File(path, "share.jpg")
    return FileProvider.getUriForFile(context, "app.ailaai.share.fileprovider", newFile)
}

suspend fun String.downloadAudio(context: Context): Uri? {
    val path = File(context.cacheDir, "share")
    path.mkdirs()
    withContext(Dispatchers.IO) {
        val stream = FileOutputStream("$path/audio.mp4")
        try {
            api.downloadFile(this@downloadAudio, stream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            stream.close()
        }
    } ?: return null

    val newFile = File(path, "audio.mp4")
    return FileProvider.getUriForFile(context, "app.ailaai.share.fileprovider", newFile)
}
suspend fun String.shareAsTextFile(context: Context, filename: String, contentType: ContentType): Boolean {
    val path = File(context.cacheDir, "share")
    path.mkdirs()
    withContext(Dispatchers.IO) {
        val stream = FileOutputStream("$path/$filename")
        try {
            stream.writer().write(this@shareAsTextFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            stream.close()
        }
    } ?: return false

    val newFile = File(path, filename)
    val uri = FileProvider.getUriForFile(context, "app.ailaai.share.fileprovider", newFile)
    shareFile(uri, context, filename, contentType)
    return true
}

suspend fun String.shareAudio(context: Context, name: String?): Boolean {
    shareFile(downloadAudio(context) ?: return false, context, name, ContentType.Audio.MP4)
    return true
}

fun <R : Any> AnnotatedString.Builder.bold(block: AnnotatedString.Builder.() -> R) =
    withStyle(SpanStyle(fontWeight = FontWeight.Bold), block)
