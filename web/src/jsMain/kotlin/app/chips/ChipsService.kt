package app.chips

import api
import app.ailaai.api.chips
import app.ailaai.api.createChip
import app.ailaai.api.deleteChip
import app.ailaai.api.previewChip
import app.ailaai.api.reorderChips
import app.ailaai.api.syncChip
import app.ailaai.api.updateChip
import com.queatz.db.ChipOrderItem
import com.queatz.db.ChipPreviewResponse
import com.queatz.db.PersonChip
import com.queatz.db.PersonChipExtended
import com.queatz.db.ReorderChipsBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val chips by lazy { ChipsService() }

class ChipsService {
    private val _items = MutableStateFlow<List<PersonChipExtended>>(emptyList())
    val items: StateFlow<List<PersonChipExtended>> = _items.asStateFlow()

    fun clear() {
        _items.value = emptyList()
    }

    suspend fun reload() {
        api.chips { fetched ->
            _items.value = fetched
        }
    }

    suspend fun create(
        chip: PersonChip,
    ): PersonChipExtended? {
        var created: PersonChipExtended? = null
        api.createChip(chip = chip) { result ->
            created = result
            _items.value = sortExtended(_items.value + result)
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
            _items.value = sortExtended(
                _items.value.map { if (it.chip.id == id) result else it }
            )
        }
        return updated
    }

    suspend fun remove(
        id: String,
    ) {
        val previous = _items.value
        _items.value = previous.filter { it.chip.id != id }
        api.deleteChip(
            id = id,
            onError = {
                _items.value = previous
            },
        )
    }

    suspend fun sync(
        id: String,
    ): Boolean {
        var success = false
        api.syncChip(id = id) { result ->
            success = result.syncedAt != null
            _items.value = _items.value.map { if (it.chip.id == id) result else it }
        }
        return success
    }

    suspend fun reorder(
        orderItems: List<ChipOrderItem>,
    ) {
        val byId = orderItems.associateBy { it.id }
        _items.value = sortExtended(
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

        api.reorderChips(
            body = ReorderChipsBody(items = orderItems),
        ) { refreshed ->
            _items.value = refreshed
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
            compareBy(
                { if (it.chip.isAbove != false) 0 else 1 },
                { it.chip.order ?: 0 },
            )
        )
}
