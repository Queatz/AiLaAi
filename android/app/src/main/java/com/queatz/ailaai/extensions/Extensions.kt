package com.queatz.ailaai.extensions

import android.content.Context
import android.net.Uri
import io.ktor.client.request.forms.*
import io.ktor.utils.io.streams.*
import java.io.File
import java.io.InputStream

fun InputStream.asInputProvider() = InputProvider(available().toLong()) { asInput() }
fun File.asInputProvider() = inputStream().asInputProvider()
fun Uri.asInputProvider(context: Context, maxSize: Int? = null) = context.contentResolver
    .openInputStream(this)
    ?.also { if (maxSize != null && it.available() > maxSize) throw FileSizeException() }
    ?.asInputProvider()

class FileSizeException : Exception()
