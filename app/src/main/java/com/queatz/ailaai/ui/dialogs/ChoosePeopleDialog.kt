package com.queatz.ailaai.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api


@Composable
fun ChoosePeopleDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<Person>) -> String,
    onPeopleSelected: suspend (List<Person>) -> Unit,
    omit: List<Person> = emptyList()
) {

    var isLoading by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var people by remember { mutableStateOf(listOf<Person>()) }
    var selected by remember { mutableStateOf(listOf<Person>()) }

    LaunchedEffect(true) {
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
            .filter { omit.none { person -> it.id == person.id } }
        people = (if (searchText.isBlank()) allPeople else allPeople.filter {
            it.name?.contains(searchText, true) ?: false
        })
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = isLoading,
        title = title,
        photoFormatter = { listOf(it.photo ?: "") },
        nameFormatter = { it.name ?: stringResource(R.string.someone) },
        confirmFormatter = confirmFormatter,
        textWhenEmpty = { isBlank -> stringResource(if (isBlank) R.string.you_have_no_conversations else R.string.no_conversations_to_show) },
        searchText = searchText,
        searchTextChange = { searchText = it },
        items = people,
        key = { it.id!! },
        selected = selected,
        onSelectedChange = { selected = it },
        onConfirm = onPeopleSelected
    )
}
