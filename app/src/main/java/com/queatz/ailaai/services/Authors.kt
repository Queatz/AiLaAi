package com.queatz.ailaai.services

import com.queatz.ailaai.api.profile
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.PersonProfile
import com.queatz.ailaai.data.api
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes


val authors by lazy {
    Authors()
}

data class Author(
    val updated: Instant,
    val person: PersonProfile
)

class Authors {

    private val authors = mutableMapOf<String, Author>()
    private val loading = mutableMapOf<String, CompletableDeferred<Unit>>()

    suspend fun person(id: String): Person? {
        loading[id]?.await()
        var author = authors[id]?.takeIf { it.updated > Clock.System.now() - 5.minutes }

        if (author == null) {
            val deferred = CompletableDeferred<Unit>()
            loading[id] = deferred
            api.profile(id) {
                Author(Clock.System.now(), it).also {
                    author = it
                    authors[id] = it
                }
            }
            deferred.complete(Unit)
        }

        return author?.person?.person
    }
}
