package com.queatz

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

fun List<PartData>.toMap() = buildMap {
    this@toMap.mapNotNull { it as? PartData.FormItem }.forEach {
        if (it.name != null) {
            put(it.name!!, it.value)
        }
    }
}


fun PipelineContext<*, ApplicationCall>.parameter(name: String) = call.parameters[name]!!

suspend fun ApplicationCall.receiveFiles(param: String, prefix: String, onFileNames: suspend (fileNames: List<String>, params: Map<String, String>) -> Unit) {
    val parts = receiveMultipart().readAllParts()

    val match = "$param\\[(\\d+)]".toRegex()
    val (fileItems, params) = parts.partition { match.matches(it.name ?: return@partition false) }.let {
        it.first.mapNotNull { it as? PartData.FileItem } to it.second.toMap()
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
                val fileName = "$prefix-${Random.nextInt(10000000, 99999999)}-${fileItem.originalFileName}"
                val file = File("$folder/$fileName")
                file.outputStream().write(fileItem.streamProvider().readBytes())
                "${folder.drop(1)}/$fileName"
            }
        }

        onFileNames(urls, params)

        HttpStatusCode.NoContent
    }
}

suspend fun ApplicationCall.receiveFile(param: String, prefix: String, onFileName: suspend (fileName: String, params: Map<String, String>) -> Unit) {
    val parts = receiveMultipart().readAllParts()

    val fileItem = parts.find { it.name == param } as? PartData.FileItem
    val params = parts.filter { it.name != param }.toMap()

    if (fileItem == null) {
        HttpStatusCode.BadRequest.description("Missing '$param'")
    } else {
        val folder = "./static/$param"
        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-${fileItem.originalFileName}"
        val file = File("$folder/$fileName")

        withContext(Dispatchers.IO) {
            file.outputStream().write(fileItem.streamProvider().readBytes())
        }

        onFileName("${folder.drop(1)}/$fileName", params)

        HttpStatusCode.NoContent
    }
}
