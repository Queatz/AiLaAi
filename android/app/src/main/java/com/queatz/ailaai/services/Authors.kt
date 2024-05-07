package com.queatz.ailaai.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.ailaai.api.profile
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.db.Person
import com.queatz.db.PersonProfile
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

    fun cached(id: String): Person? = getCached(id)?.person?.person

    @Composable
    fun get(id: String): Person? {
        var person by remember(id) { mutableStateOf(cached(id)) }

        LaunchedEffect(id) {
            if (person == null) {
                person = this@Authors.person(id)
            }
        }

        return person
    }

    suspend fun person(id: String): Person? {
        loading[id]?.await()
        var author = getCached(id)

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

    private fun getCached(id: String): Author? {
        return authors[id]?.takeIf { it.updated > Clock.System.now() - 5.minutes }
    }
}
