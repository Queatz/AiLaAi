package com.queatz.ailaai.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

suspend fun Uri.asScaledJpeg(context: Context, longestEdge: Int = 1600): ByteArray {
    return withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(this@asScaledJpeg))
        val w = if (bitmap.width > bitmap.height) longestEdge else (longestEdge * bitmap.aspect).toInt()
        val h = if (bitmap.height > bitmap.width) longestEdge else (longestEdge / bitmap.aspect).toInt()
        val scaled =
            if (w < bitmap.width || h < bitmap.height) Bitmap.createScaledBitmap(bitmap, w, h, true) else bitmap
        ByteArrayOutputStream(scaled.byteCount / 8).let {
            scaled.compress(Bitmap.CompressFormat.JPEG, 92, it)
            it.toByteArray()
        }
    }
}

suspend fun Uri.asScaledVideo(context: Context, progressCallback: (Float) -> Unit): InputStream {
    return withContext(Dispatchers.IO) {
        val outputFile = File.createTempFile("video", ".webm", context.cacheDir)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val deferred = CompletableDeferred<Unit>()
        Transcoder.into(outputFile.path)
            .addDataSource(context, this@asScaledVideo)
            .setVideoTrackStrategy(DefaultVideoStrategy.atMost(1280).frameRate(30).build())
            .setAudioTrackStrategy(DefaultAudioStrategy.builder().channels(2).build())
            .setListener(object : TranscoderListener {
                override fun onTranscodeProgress(progress: Double) {
                    progressCallback(progress.toFloat())
                }

                override fun onTranscodeCompleted(successCode: Int) {
                    deferred.complete(Unit)
                }

                override fun onTranscodeCanceled() {
                    deferred.complete(Unit)
                }

                override fun onTranscodeFailed(exception: Throwable) {
                    deferred.completeExceptionally(exception)
                }
            }).transcode()
        deferred.await()
        outputFile.inputStream()
    }
}

val Bitmap.aspect get() = width.toFloat() / height.toFloat()
