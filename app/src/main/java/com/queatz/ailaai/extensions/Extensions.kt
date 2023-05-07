package com.queatz.ailaai.extensions

import io.ktor.client.request.forms.*
import io.ktor.utils.io.streams.*
import java.io.File
import java.io.InputStream

fun InputStream.asInputProvider() = InputProvider(available().toLong()) { asInput() }
fun File.asInputProvider() = inputStream().asInputProvider()
