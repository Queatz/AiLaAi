package com.queatz.ailaai.ui.dialogs

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.toggle
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CreateGroupDialog(onDismissRequest: () -> Unit, onNewGroup: (Group) -> Unit, me: () -> Person?) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    var disableSubmit by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
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
        val me = me()
        val allPeople = allGroups
            .flatMap { it.members!!.map { it.person!! } }
            .distinctBy { it.id!! }
            .filter { it.id != me?.id }
        people = (if (searchText.isBlank()) allPeople else allPeople.filter {
            it.name?.contains(searchText, true) ?: false
        })
    }

    LaunchedEffect(selected) {
        disableSubmit = selected.isEmpty()
    }

    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
                .fillMaxHeight(.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault * 3)
            ) {
                Text(
                    stringResource(R.string.new_group),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = PaddingDefault)
                )
                OutlinedTextField(
                    searchText,
                    onValueChange = { searchText = it },
                    label = { Text(stringResource(R.string.search)) },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    if (isLoading) {
                        item {
                            LinearProgressIndicator(
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PaddingDefault)
                            )
                        }
                    } else if (people.isEmpty()) {
                        item {
                            Text(
                                stringResource(if (searchText.isBlank()) R.string.you_have_no_conversations else R.string.no_conversations_to_show),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(PaddingDefault * 2)
                            )
                        }
                    } else {
                        items(people, key = { it.id!! }) {
                            val isSelected = selected.any { person -> person.id == it.id }
                            val backgroundColor by animateColorAsState(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            val contentColor by animateColorAsState(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .background(backgroundColor)
                                    .clickable {
                                        selected = selected.toggle(it) { person -> person.id == it.id }
                                    }) {
                                AsyncImage(
                                    model = it.photo?.let { api.url(it) } ?: "",
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .padding(PaddingDefault)
                                        .requiredSize(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                )
                                Text(
                                    "${it.name}",
                                    color = contentColor,
                                    modifier = Modifier
                                        .padding(PaddingDefault)
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val context = LocalContext.current
                    val didntWork = stringResource(R.string.didnt_work)
                    TextButton(
                        {
                            disableSubmit = true

                            coroutineScope.launch {
                                try {
                                    val group = api.createGroup(selected.map { it.id!! })
                                    onNewGroup(group)
                                    onDismissRequest()
                                } catch (ex: Exception) {
                                    Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                                    ex.printStackTrace()
                                } finally {
                                    disableSubmit = false
                                }
                            }
                        },
                        enabled = !disableSubmit,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(newGroupText(selected), textAlign = TextAlign.End, modifier = Modifier.weight(0.5f, false))
                        Icon(Icons.Outlined.ArrowForward, "", modifier = Modifier.padding(start = PaddingDefault))
                    }
                }
            }
        }
    }
}

@Composable
fun newGroupText(selected: List<Person>) = when {
    selected.isEmpty() -> stringResource(R.string.new_group)
    selected.size == 1 -> stringResource(R.string.new_group_with_person, *selected.map { it.name!! }.toTypedArray())
    selected.size == 2 -> stringResource(R.string.new_group_with_people, *selected.map { it.name!! }.toTypedArray())
    else -> stringResource(R.string.new_group_with_x_people, selected.size)
}
