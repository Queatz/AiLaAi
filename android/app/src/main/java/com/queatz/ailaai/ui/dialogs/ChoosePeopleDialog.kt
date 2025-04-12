package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.ailaai.api.groups
import app.ailaai.api.profile
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.components.ScanQrCodeResult
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import com.queatz.db.PersonSource
import kotlinx.coroutines.launch

@Composable
fun ChoosePeopleDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmFormatter: @Composable (List<Person>) -> String,
    people: List<Person>? = null,
    allowNone: Boolean = false,
    multiple: Boolean = true,
    initiallySelected: List<Person> = listOf(),
    onPeopleSelected: suspend (List<Person>) -> Unit,
    extraButtons: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    omit: (Person) -> Boolean = { false }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by rememberStateOf(false)
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var scannedPeople by remember { mutableStateOf(listOf<Person>()) }
    var shownPeople by remember { mutableStateOf(listOf<Person>()) }
    var selected by remember { mutableStateOf(initiallySelected) }
    val state = rememberLazyListState()

    if (people != null) {
        LaunchedEffect(Unit) {
            shownPeople = people.filter { !omit(it) }
        }
    } else {
        LaunchedEffect(Unit) {
            isLoading = true
            api.groups {
                allGroups = it
            }
            isLoading = false
        }

        LaunchedEffect(allGroups, scannedPeople, selected, searchText) {
            val allPeople = scannedPeople + allGroups
                .flatMap { it.members!!.map { it.person!! } }
                .filter { it.source != PersonSource.Web }
                .filter { !omit(it) }
            shownPeople = (if (searchText.isBlank()) allPeople else allPeople.filter {
                it.name?.contains(searchText, true) == true
            })
                .distinctBy { it.id!! }
        }
    }

    LaunchedEffect(scannedPeople) {
        state.scrollToTop()
    }

    ChooseDialog(
        onDismissRequest = onDismissRequest,
        isLoading = isLoading,
        title = title,
        allowNone = allowNone,
        extraButtons = extraButtons,
        actions = actions,
        photoFormatter = { listOf(ContactPhoto(it.name ?: "", it.photo, it.seen)) },
        nameFormatter = { it.name ?: stringResource(R.string.someone) },
        infoFormatter = {
            it.seen?.timeAgo()?.let { timeAgo ->
                "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
            }
        },
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
        state = state,
        onQrCodeScan = {
            when (it) {
                is ScanQrCodeResult.Profile -> {
                    scope.launch {
                        api.profile(it.id) {
                            val person = it.person
                            scannedPeople += person
                            if (selected.none { it.id == person.id }) {
                                selected = selected + it.person
                                context.toast(context.getString(R.string.x_selected, person.name ?: context.getString(R.string.someone)))
                            }
                        }
                    }
                }

                else -> {
                    context.showDidntWork()
                }
            }
        },
        onConfirm = onPeopleSelected
    )
}
