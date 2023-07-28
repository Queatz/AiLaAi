package com.queatz.ailaai.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.queatz.ailaai.extensions.nullIfBlank
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

val say by lazy {
    Say()
}

class Say {
    private val says = MutableStateFlow<String?>(null)
    private var job: Job? = null

    @Composable
    fun rememberSays(): State<String?> = says.collectAsState()

    suspend fun say(say: String?) {
        try {
            synchronized(this) { job }?.cancelAndJoin()
        } finally {
            say?.nullIfBlank?.let { say ->
                coroutineScope {
                    synchronized(this) {
                        job = launch {
                            try {
                                says.value = say
                                delay(2_000)
                            } finally {
                                says.value = null
                            }
                        }
                    }
                }
                synchronized(this) { job }?.join()
            } ?: run {
                synchronized(this) {
                    job = null
                }
            }
        }
    }
}
