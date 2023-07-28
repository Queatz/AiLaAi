package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.categories
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.helpers.geoKey
import kotlinx.coroutines.flow.first

@Composable
fun ChooseCategoryDialog(
    onDismissRequest: () -> Unit,
    onCategory: (String?) -> Unit
) {
    val context = LocalContext.current
    var isLoading by rememberStateOf(false)
    var searchText by remember { mutableStateOf("") }
    var allCategories by remember { mutableStateOf(listOf<String>()) }
    var categories by remember { mutableStateOf(listOf<String>()) }
    var selected by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        val savedGeo = context.dataStore.data.first()[geoKey]?.split(",")?.map { it.toDouble() } // todo user LocationSelector
        if (savedGeo == null) {
            onDismissRequest()
            return@LaunchedEffect
        }

        val geo = LatLng.getFactory().create(savedGeo[0], savedGeo[1])

        isLoading = true
        api.categories(geo) {
            allCategories = it
        }
        isLoading = false
    }

    LaunchedEffect(allCategories, selected, searchText) {
        categories = (if (searchText.isBlank()) allCategories else allCategories.filter {
            it.contains(searchText, true)
        })
    }

    LaunchedEffect(searchText) {
        selected = emptyList()
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = isLoading,
        title = stringResource(R.string.set_category),
        allowNone = true,
        photoFormatter = null,
        nameFormatter = { it },
        confirmFormatter = {
            when {
                it.isEmpty() && searchText.isBlank() -> stringResource(R.string.choose_none)
                it.isEmpty() -> stringResource(R.string.choose_x_quoted, searchText)
                else -> stringResource(R.string.choose_x_quoted, it.first())
            }
        },
        textWhenEmpty = { "" },
        searchText = searchText,
        searchTextChange = { searchText = it },
        items = categories,
        key = { it },
        selected = selected,
        onSelectedChange = {
            selected = it - selected
        },
        showSearch = { true },
        onConfirm = {
            onCategory(it.firstOrNull() ?: searchText.takeIf { it.isNotBlank() })
        }
    )
}
