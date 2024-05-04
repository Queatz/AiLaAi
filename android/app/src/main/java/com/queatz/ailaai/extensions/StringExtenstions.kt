package com.queatz.ailaai.extensions

import android.content.*
import android.graphics.Bitmap
import android.icu.text.DecimalFormatSymbols
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ibm.icu.math.BigDecimal
import com.queatz.ailaai.data.api
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun String.isNumericTextInput(allowDecimal: Boolean = true): Boolean {
    return isEmpty() || if (allowDecimal) (this == DecimalFormatSymbols.getInstance().decimalSeparator.toString() || toItemQuantity() != null) else toIntOrNull() != null
}

fun String.wordCount() = if (isBlank()) 0 else trim().split("\\W+".toRegex()).size

fun bulletedString(vararg items: String?) = items.filterNotNull().joinToString(" • ")

val String.ensureScheme get() = if (contains("://")) this else "https://$this"

fun String.launchUrl(context: Context) {
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(ensureScheme)
            )
        )
    } catch (t: Throwable) {
        context.showDidntWork()
    }
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

fun String.asCacheFileUri(context: Context): Uri {
    val path = File(context.cacheDir, "share")
    path.mkdirs()
    val newFile = File(path, this)
    return FileProvider.getUriForFile(context, "app.ailaai.share.fileprovider", newFile)
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

suspend fun Bitmap.save(context: Context): Uri? {
    val filename = "ailaai_photo_${Clock.System.now()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")// todo use PNG if it has transparency
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        } else {
            put(
                MediaStore.MediaColumns.DATA,
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath}${File.separator}$filename"
            )
        }
    }

    val contentResolver = context.contentResolver

    val uri = withContext(Dispatchers.IO) {
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.also {
            contentResolver.openOutputStream(it)?.use {
                compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
        }
    } ?: return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)

        contentResolver.update(uri, contentValues, null, null)
    }

    return uri
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

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)
