package com.queatz.ailaai.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import app.ailaai.api.activeSignals
import com.queatz.ailaai.cache.CacheKey
import com.queatz.ailaai.cache.cache
import com.queatz.ailaai.data.api
import com.queatz.db.ActiveSignalsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

val signals by lazy {
    Signals()
}

class Signals {
    private val _activeSignals = MutableStateFlow(loadCached())
    val activeSignals = _activeSignals.asStateFlow()

    private fun loadCached(): ActiveSignalsResponse? {
        return cache.get<ActiveSignalsResponse>(CacheKey.ActiveSignals)?.filterExpired()
    }

    private fun ActiveSignalsResponse.filterExpired(): ActiveSignalsResponse {
        val now = Clock.System.now()
        return ActiveSignalsResponse(
            mine = mine.filter { it.signalSend.expiry == null || it.signalSend.expiry!! > now },
            others = others.filter { it.signalSend.expiry == null || it.signalSend.expiry!! > now }
        )
    }

    @Composable
    fun rememberActiveSignals(): State<ActiveSignalsResponse?> = activeSignals.collectAsState()

    fun update(response: ActiveSignalsResponse) {
        _activeSignals.value = response
        cache.put(CacheKey.ActiveSignals, response)
    }

    fun clear() {
        _activeSignals.value = null
        cache.remove(CacheKey.ActiveSignals)
    }

    suspend fun reload(geo: List<Double>? = null, onError: suspend (Throwable) -> Unit = {}) {
        api.activeSignals(geo = geo, onError = onError) {
            if (geo == null) {
                update(it)
            }
        }
    }
}
