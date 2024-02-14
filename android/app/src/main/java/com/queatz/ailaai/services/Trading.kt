package com.queatz.ailaai.services

import com.queatz.ailaai.data.api
import com.queatz.db.TradeExtended
import com.queatz.push.TradePushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import trades
import kotlin.time.Duration.Companion.seconds

val trading by lazy {
    Trading()
}

class Trading {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val _activeTrades = MutableStateFlow(emptyList<TradeExtended>())

    val activeTrades = _activeTrades.asStateFlow()

    init {
        scope.launch {
            delay(2.seconds)
            push.events
                .mapNotNull { it as? TradePushData }
                .catch { it.printStackTrace() }
                .collectLatest {
                    reload()
                }
        }
    }

    suspend fun reload() {
        api.trades {
            _activeTrades.value = it
        }
    }
}
