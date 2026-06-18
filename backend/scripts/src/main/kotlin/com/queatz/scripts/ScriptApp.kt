package com.queatz.scripts

interface ScriptApp {

    suspend fun download(
        url: String,
        name: String = url.substringAfterLast('/'),
    ): String

}
