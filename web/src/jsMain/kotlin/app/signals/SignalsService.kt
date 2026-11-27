package app.signals

import api
import app.ailaai.api.activeSignals
import app.ailaai.api.mySignals
import app.ailaai.api.signals
import app.ailaai.api.toggleSignal
import com.queatz.db.ActiveSignalsResponse
import com.queatz.db.PersonSignal
import com.queatz.db.Signal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.js.Date
import kotlin.time.Clock

val signals by lazy { SignalsService() }

enum class SignalsNavFilter {
    All,
    MineOn,
    Active,
}

class SignalsService {
    private val _activeSignals = MutableStateFlow<ActiveSignalsResponse?>(null)
    val activeSignals: StateFlow<ActiveSignalsResponse?> = _activeSignals.asStateFlow()

    private val _personSignals = MutableStateFlow<List<PersonSignal>>(emptyList())
    val personSignals: StateFlow<List<PersonSignal>> = _personSignals.asStateFlow()

    private val _catalog = MutableStateFlow<List<Signal>>(emptyList())
    val catalog: StateFlow<List<Signal>> = _catalog.asStateFlow()

    private val _navFilter = MutableStateFlow(SignalsNavFilter.All)
    val navFilter: StateFlow<SignalsNavFilter> = _navFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    fun setNavFilter(
        filter: SignalsNavFilter,
    ) {
        _navFilter.value = filter
    }

    fun setSelectedCategory(
        category: String?,
    ) {
        _selectedCategory.value = category
    }

    fun setSearchText(
        text: String,
    ) {
        _searchText.value = text
    }

    fun clear() {
        _activeSignals.value = null
        _personSignals.value = emptyList()
    }

    suspend fun reloadActive(
        geo: List<Double>? = null,
    ) {
        api.activeSignals(geo = geo) { response ->
            val now = Clock.System.now()
            _activeSignals.value = ActiveSignalsResponse(
                mine = response.mine.filter { it.signalSend.expiry == null || it.signalSend.expiry!! > now },
                others = response.others.filter { it.signalSend.expiry == null || it.signalSend.expiry!! > now },
            )
        }
    }

    suspend fun reloadMySignals() {
        api.mySignals { list ->
            _personSignals.value = list
        }
    }

    suspend fun loadCatalog(
        offset: Int = 0,
        reset: Boolean = false,
    ): Boolean {
        var hasMore = false
        val localHour = Date().getHours()
        api.signals(
            localHour = localHour,
            offset = offset,
        ) { page ->
            _catalog.value = if (reset) page else (_catalog.value + page).distinctBy { it.id }
            hasMore = page.size >= 20
        }
        return hasMore
    }

    fun addCreatedSignal(
        signal: Signal,
    ) {
        _catalog.value = (_catalog.value + signal).distinctBy { it.id }.sortedBy { it.name }
    }

    suspend fun toggle(
        signalId: String,
    ): PersonSignal? {
        var updated: PersonSignal? = null
        api.toggleSignal(id = signalId) { ps ->
            updated = ps
            _personSignals.value = _personSignals.value.filter { it.signal != signalId } + ps
        }
        return updated
    }
}
