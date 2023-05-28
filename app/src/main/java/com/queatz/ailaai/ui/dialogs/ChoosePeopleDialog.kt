package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.rememberStateOf


@Composable
fun ChoosePeopleDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<Person>) -> String,
    allowNone: Boolean = false,
    multiple: Boolean = true,
    onPeopleSelected: suspend (List<Person>) -> Unit,
    extraButtons: @Composable RowScope.() -> Unit = {},
    omit: (Person) -> Boolean = { false }
) {
    var isLoading by rememberStateOf(false)
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var people by remember { mutableStateOf(listOf<Person>()) }
    var selected by remember { mutableStateOf(listOf<Person>()) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            allGroups = api.groups()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isLoading = false
    }

    LaunchedEffect(allGroups, selected, searchText) {
        val allPeople = allGroups
            .flatMap { it.members!!.map { it.person!! } }
            .distinctBy { it.id!! }
            .filter { it.source != PersonSource.Web }
            .filter { !omit(it) }
        people = (if (searchText.isBlank()) allPeople else allPeople.filter {
            it.name?.contains(searchText, true) ?: false
        })
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = isLoading,
        title = title,
        allowNone = allowNone,
        extraButtons = extraButtons,
        photoFormatter = { listOf(ContactPhoto(it.name ?: "", it.photo)) },
        nameFormatter = { it.name ?: stringResource(R.string.someone) },
        confirmFormatter = confirmFormatter,
        textWhenEmpty = { stringResource(R.string.no_people_to_show) },
        searchText = searchText,
        searchTextChange = { searchText = it },
        items = people,
        key = { it.id!! },
        selected = selected,
        onSelectedChange = {
            selected = if (multiple) {
                it
            } else {
                it - selected
            }
        },
        onConfirm = onPeopleSelected
    )
}
