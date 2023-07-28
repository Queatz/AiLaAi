package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.api.groups
import com.queatz.ailaai.data.GroupExtended
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.PersonSource
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.rememberStateOf


@Composable
fun ChoosePeopleDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<Person>) -> String,
    people: List<Person>? = null,
    allowNone: Boolean = false,
    multiple: Boolean = true,
    onPeopleSelected: suspend (List<Person>) -> Unit,
    extraButtons: @Composable RowScope.() -> Unit = {},
    omit: (Person) -> Boolean = { false }
) {
    var isLoading by rememberStateOf(false)
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var shownPeople by remember { mutableStateOf(listOf<Person>()) }
    var selected by remember { mutableStateOf(listOf<Person>()) }

    if (people != null) {
        LaunchedEffect(Unit) {
            shownPeople = people
        }
    } else {
        LaunchedEffect(Unit) {
            isLoading = true
            api.groups {
                allGroups = it
            }
            isLoading = false
        }

        LaunchedEffect(allGroups, selected, searchText) {
            val allPeople = allGroups
                .flatMap { it.members!!.map { it.person!! } }
                .distinctBy { it.id!! }
                .filter { it.source != PersonSource.Web }
                .filter { !omit(it) }
            shownPeople = (if (searchText.isBlank()) allPeople else allPeople.filter {
                it.name?.contains(searchText, true) ?: false
            })
        }
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
        items = shownPeople,
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
