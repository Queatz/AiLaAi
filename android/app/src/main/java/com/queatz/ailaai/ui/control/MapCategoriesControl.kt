package com.queatz.ailaai.ui.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.sortedDistinct
import com.queatz.db.Card
import kotlinx.coroutines.launch

data class MapCategoriesControl(
    val updateCategories: () -> Unit,
    val selectedCategory: String?,
    val categories: List<String>,
    val selectCategory: (String?) -> Unit,
    val cardsOfCategory: List<Card>
)

@Composable
fun mapCategoriesControl(
    cards: List<Card>
): MapCategoriesControl {
    val scope = rememberCoroutineScope()
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by rememberSaveable { mutableStateOf(emptyList<String>()) }

    fun updateCategories() {
        scope.launch {
            selectedCategory = selectedCategory?.notBlank
            categories = cards.flatMap { it.categories ?: emptyList() } + (
                    selectedCategory?.inList()?.sortedDistinct()
                        ?: emptyList()
                    )
        }
    }

    val cardsOfCategory = remember(cards, selectedCategory) {
        if (selectedCategory == null) cards else cards.filter {
            it.categories?.contains(
                selectedCategory
            ) == true
        }
    }

    return MapCategoriesControl(
        updateCategories = { updateCategories() },
        categories = categories,
        selectedCategory = selectedCategory,
        selectCategory = {
            selectedCategory = it
        },
        cardsOfCategory = cardsOfCategory
    )
}
