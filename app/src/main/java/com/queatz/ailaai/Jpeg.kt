package com.queatz.ailaai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

fun Uri.asScaledJpeg(context: Context, longestEdge: Int = 1200): ByteArray {
    val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(this))
    val w = if (bitmap.width > bitmap.height) longestEdge else (longestEdge * bitmap.aspect).toInt()
    val h = if (bitmap.height > bitmap.width) longestEdge else (longestEdge / bitmap.aspect).toInt()
    val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
    return ByteArrayOutputStream(scaled.byteCount / 8).let {
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, it)
        it.toByteArray()
    }
}

val Bitmap.aspect get() = width.toFloat() / height.toFloat()
