package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.Presence
import io.ktor.http.*

suspend fun Api.presence(): Presence = get("me/presence")
suspend fun Api.readStoriesUntilNow(): HttpStatusCode = post("me/presence/read-stories")
