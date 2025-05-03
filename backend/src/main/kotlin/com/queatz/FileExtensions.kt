package com.queatz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

suspend fun ByteArray.save(path: String, fileName: String): String {
    val folder = "./static/$path"

    if (!File(folder).isDirectory) {
        File(folder).mkdirs()
    }

    val fileName = "file-${Random.nextInt(100_000_000, 999_999_999)}-$fileName"
    val file = File("$folder/$fileName")

    withContext(Dispatchers.IO) {
        file.writeBytes(this@save)
    }

    return "${folder.drop(1)}/$fileName"
}
