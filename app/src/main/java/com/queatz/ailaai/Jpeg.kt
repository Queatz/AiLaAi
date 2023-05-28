package com.queatz.ailaai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

suspend fun Uri.asScaledJpeg(context: Context, longestEdge: Int = 1200): ByteArray {
    return withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(this@asScaledJpeg))
        val w = if (bitmap.width > bitmap.height) longestEdge else (longestEdge * bitmap.aspect).toInt()
        val h = if (bitmap.height > bitmap.width) longestEdge else (longestEdge / bitmap.aspect).toInt()
        val scaled =
            if (w < bitmap.width || h < bitmap.height) Bitmap.createScaledBitmap(bitmap, w, h, true) else bitmap
        ByteArrayOutputStream(scaled.byteCount / 8).let {
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, it)
            it.toByteArray()
        }
    }
}

suspend fun Uri.asScaledVideo(context: Context, progressCallback: (Float) -> Unit): InputStream {
    return withContext(Dispatchers.IO) {
        val outputFile = File.createTempFile("video", ".mkv", context.cacheDir)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val mediaInformation =
            FFprobeKit.getMediaInformation(FFmpegKitConfig.getSafParameterForRead(context, this@asScaledVideo))
        if (ReturnCode.isSuccess(mediaInformation.returnCode)) {
            val deferred = CompletableDeferred<Unit>()
            val duration = mediaInformation.mediaInformation.duration.toFloat()
            val inputVideoPath = FFmpegKitConfig.getSafParameterForRead(context, this@asScaledVideo)
            val session = FFmpegKit.executeAsync(
                "-i $inputVideoPath -c:v libx265 -vtag hvc1 -vf scale=720:-2 -crf 42 ${outputFile.path}",
                {
                    deferred.complete(Unit)
                },
                {
                    println(it.message)
                },
                { statistics ->
                    progressCallback(statistics.time / (duration * 1_000f))
                }
            )
            try {
                deferred.await()
            } finally {
                if (!ReturnCode.isSuccess(session.returnCode)) {
                    FFmpegKit.cancel(session.sessionId)
                }
            }
            if (!ReturnCode.isSuccess(session.returnCode)) {
                throw RuntimeException("Process video didn't work")
            }
            outputFile.inputStream()
        } else {
            throw RuntimeException("Read media info didn't work")
        }
    }
}


val Bitmap.aspect get() = width.toFloat() / height.toFloat()
