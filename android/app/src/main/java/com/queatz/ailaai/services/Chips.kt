package com.queatz.ailaai.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ailaai.api.chips
import app.ailaai.api.createChip
import app.ailaai.api.deleteChip
import app.ailaai.api.importChips
import app.ailaai.api.previewChip
import app.ailaai.api.reorderChips
import app.ailaai.api.syncChip
import app.ailaai.api.updateChip
import com.queatz.ailaai.cache.CacheKey
import com.queatz.ailaai.cache.cache
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.db.ChipFormatType
import com.queatz.db.ChipOrderItem
import com.queatz.db.ChipPreviewResponse
import com.queatz.db.ImportChipsBody
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.ReorderChipsBody
import com.queatz.db.defaultChipHexColor
import com.queatz.db.legacyColorIntToHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LegacyChipConfig(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val type: ChipFormatType = ChipFormatType.TEXT,
    val prefix: String = "",
    val backgroundColor: Int = 0,
    val isAbove: Boolean = true,
    val jsonPath: String = "",
)

private val legacyJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

val chips by lazy { Chips() }

class Chips {
    private lateinit var context: Context
    private val scope = CoroutineScope(Dispatchers.IO)
    private val legacyConfigKey = stringPreferencesKey("app.chips_config")
    private val legacyCacheKey = stringPreferencesKey("app.chips_cache")

    private val _items = MutableStateFlow<List<PersonChipExtended>>(emptyList())
    val items: StateFlow<List<PersonChipExtended>> = _items.asStateFlow()

    fun init(
        context: Context,
    ) {
        this.context = context
        cache.get<List<PersonChipExtended>>(CacheKey.Chips)?.let {
            _items.value = it
        }
    }

    private fun updateState(
        list: List<PersonChipExtended>,
    ) {
        _items.value = list
        cache.put(CacheKey.Chips, list)
    }

    fun clear() {
        _items.value = emptyList()
        cache.remove(CacheKey.Chips)
    }

    suspend fun reload() {
        api.chips { fetched ->
            if (fetched.isEmpty() && ::context.isInitialized) {
                val migrated = migrateLegacyConfigsIfPresent()
                if (migrated != null) {
                    updateState(migrated)
                    return@chips
                }
            }
            updateState(fetched)
        }
    }

    private suspend fun migrateLegacyConfigsIfPresent(): List<PersonChipExtended>? {
        val prefs = runCatching { context.dataStore.data.first() }.getOrNull() ?: return null
        val raw = prefs[legacyConfigKey]?.takeIf { it.isNotBlank() } ?: return null
        val legacyList = runCatching { legacyJson.decodeFromString<List<LegacyChipConfig>>(raw) }.getOrNull()
        if (legacyList.isNullOrEmpty()) {
            clearLegacyPrefs()
            return null
        }

        val toImport = legacyList.mapIndexed { index, legacy ->
            PersonChip(
                name = legacy.name,
                url = legacy.url,
                format = legacy.type,
                prefix = legacy.prefix,
                color = legacyColorIntToHex(legacy.backgroundColor) ?: defaultChipHexColor,
                jsonPath = legacy.jsonPath,
                isAbove = legacy.isAbove,
                order = index,
            )
        }

        var imported: List<PersonChipExtended>? = null
        api.importChips(
            body = ImportChipsBody(chips = toImport),
        ) { result ->
            imported = result
            scope.launch { clearLegacyPrefs() }
        }
        return imported
    }

    private suspend fun clearLegacyPrefs() {
        runCatching {
            context.dataStore.edit {
                it.remove(legacyConfigKey)
                it.remove(legacyCacheKey)
            }
        }
    }

    suspend fun create(
        chip: PersonChip,
    ): PersonChipExtended? {
        var created: PersonChipExtended? = null
        api.createChip(chip = chip) { result ->
            created = result
            updateState(sortExtended(_items.value + result))
        }
        return created
    }

    suspend fun update(
        chip: PersonChip,
    ): PersonChipExtended? {
        val id = chip.id ?: return null
        var updated: PersonChipExtended? = null
        api.updateChip(id = id, chip = chip) { result ->
            updated = result
            updateState(
                sortExtended(
                    _items.value.map { if (it.chip.id == id) result else it }
                )
            )
        }
        return updated
    }

    suspend fun remove(
        id: String,
    ) {
        val previous = _items.value
        updateState(previous.filter { it.chip.id != id })
        api.deleteChip(
            id = id,
            onError = {
                updateState(previous)
            },
        )
    }

    suspend fun sync(
        id: String,
    ): Boolean {
        var success = false
        api.syncChip(id = id) { result ->
            success = result.syncedAt != null
            updateState(
                _items.value.map { if (it.chip.id == id) result else it }
            )
        }
        return success
    }

    suspend fun reorder(
        orderItems: List<ChipOrderItem>,
    ) {
        val byId = orderItems.associateBy { it.id }
        val optimistic = sortExtended(
            _items.value.map { ext ->
                val spec = byId[ext.chip.id]
                if (spec != null) {
                    ext.copy(
                        chip = PersonChip(
                            person = ext.chip.person,
                            name = ext.chip.name,
                            url = ext.chip.url,
                            format = ext.chip.format,
                            prefix = ext.chip.prefix,
                            color = ext.chip.color,
                            jsonPath = ext.chip.jsonPath,
                            isAbove = spec.isAbove,
                            order = spec.order,
                        ).apply {
                            id = ext.chip.id
                            createdAt = ext.chip.createdAt
                        }
                    )
                } else {
                    ext
                }
            }
        )
        updateState(optimistic)

        api.reorderChips(
            body = ReorderChipsBody(items = orderItems),
        ) { refreshed ->
            updateState(refreshed)
        }
    }

    suspend fun previewUrl(
        url: String,
    ): ChipPreviewResponse? {
        if (url.isBlank()) return null
        var response: ChipPreviewResponse? = null
        api.previewChip(url = url) {
            response = it
        }
        return response
    }

    private fun sortExtended(
        list: List<PersonChipExtended>,
    ): List<PersonChipExtended> =
        list.sortedWith(
            compareBy<PersonChipExtended>(
                { if (it.chip.isAbove != false) 0 else 1 },
                { it.chip.order ?: 0 },
            )
        )
}
