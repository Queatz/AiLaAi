package com.queatz.scripts

import com.queatz.db.Message

interface ScriptApp {

    suspend fun download(
        url: String,
        name: String = url.substringAfterLast('/'),
    ): String

    suspend fun message(
        groupId: String,
        text: String,
    ): Message

}
