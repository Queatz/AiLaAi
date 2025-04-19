package com.queatz

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.RoutingContext
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import java.io.File
import kotlin.random.Random

suspend fun RoutingContext.launch(block: suspend CoroutineScope.() -> Unit) = coroutineScope {
    this.launch(block = block)
}

fun RoutingContext.parameter(name: String) = call.parameters[name]!!

suspend fun ApplicationCall.receiveBytes(
    param: String,
    onBytes: suspend (bytes: ByteArray, params: Map<String, String>) -> Unit,
) {
    var bytes: ByteArray? = null
    val params = mutableMapOf<String, String>()
    receiveMultipart().forEachPart { part ->
        if (part.name == param && part is PartData.FileItem) {
            if (bytes == null) {
                bytes = part.readBytes()
            }
        } else {
            (part as? PartData.FormItem)?.let {
                params.put(it.name ?: return@let, it.value)
            }
        }
        part.dispose()
    }

    bytes?.let {
        onBytes(it, params)
    }
}

suspend fun ApplicationCall.receiveFiles(
    param: String,
    prefix: String,
    onFileNames: suspend (fileNames: List<String>, params: Map<String, String>) -> Unit
) {
    val match = "$param\\[(\\d+)]".toRegex()
    val fileItems = mutableListOf<Pair<ByteArray, String?>>()
    val params = mutableMapOf<String, String>()
    receiveMultipart().forEachPart { part ->
        if (match.matches(part.name.orEmpty())) {
            (part as? PartData.FileItem)?.let { file ->
                fileItems += file.readBytes() to file.originalFileName
            }
        } else {
            (part as? PartData.FormItem)?.let {
                params.put(it.name ?: return@let, it.value)
            }
        }
        part.dispose()
    }

    if (fileItems.isEmpty()) {
        HttpStatusCode.BadRequest.description("Missing '$param[0]'")
    } else {
        val folder = "./static/$param"
        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val urls = withContext(Dispatchers.IO) {
            fileItems.map { fileItem ->
                val fileName = "$prefix-${Random.nextInt(10000000, 99999999)}-${fileItem.second}"
                val file = File("$folder/$fileName")
                file.writeBytes(fileItem.first)
                "${folder.drop(1)}/$fileName"
            }
        }

        onFileNames(urls, params)

        HttpStatusCode.NoContent
    }
}

suspend fun ApplicationCall.receiveFile(
    param: String,
    prefix: String,
    onFileName: suspend (fileName: String, params: Map<String, String>) -> Unit,
) {
    var fileItem: Pair<ByteArray, String?>? = null
    val params = mutableMapOf<String, String>()
    receiveMultipart().forEachPart { part ->
        if (part.name == param && part is PartData.FileItem) {
            if (fileItem == null) {
                fileItem = part.readBytes() to part.originalFileName
            }
        } else {
            (part as? PartData.FormItem)?.let {
                params.put(it.name ?: return@let, it.value)
            }
        }
        part.dispose()
    }

    if (fileItem == null) {
        HttpStatusCode.BadRequest.description("Missing '$param'")
    } else {
        val folder = "./static/$param"
        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-${fileItem!!.second}"
        val file = File("$folder/$fileName")

        withContext(Dispatchers.IO) {
            file.writeBytes(fileItem!!.first)
        }

        onFileName("${folder.drop(1)}/$fileName", params.toMap())

        HttpStatusCode.NoContent
    }
}

private suspend fun PartData.FileItem.readBytes() = provider().readRemaining().readByteArray()
