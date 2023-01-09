package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.dialogs.CreateGroupDialog
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavController, me: () -> Person?) {
    var isLoading by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var allGroups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var groups by remember { mutableStateOf(listOf<GroupExtended>()) }
    var showCreateGroup by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        isLoading = true
        try {
            allGroups = api.groups()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isLoading = false
    }

    LaunchedEffect(searchText) {
        // todo search server, set allGroups
    }

    LaunchedEffect(allGroups, searchText) {
        groups = if (searchText.isBlank()) allGroups else allGroups.filter {
            it.members?.any { it.person?.name?.contains(searchText, true) ?: false } ?: false
        }
    }

    Column {
        TopAppBar(
            {
                Text(stringResource(R.string.your_conversations), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            actions = {
                ElevatedButton(
                    {
                        showCreateGroup = true
                    },
                    modifier = Modifier.padding(horizontal = PaddingDefault)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        stringResource(R.string.new_group),
                        modifier = Modifier.padding(end = PaddingDefault)
                    )
                    Text(stringResource(R.string.new_group))
                }
            }
        )
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
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
                } else if (groups.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (searchText.isBlank()) R.string.you_have_no_conversations else R.string.no_conversations_to_show),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                } else {
                    items(groups, key = { it.group!!.id!! }) {
                        ContactItem(navController, it, me())
                    }
                }
            }
            SearchField(
                searchText, { searchText = it }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaddingDefault * 2)
            )
        }
    }

    if (showCreateGroup) {
        CreateGroupDialog({
            showCreateGroup = false
        }, {
           navController.navigate("group/${it.id!!}")
        }, me)
    }
}
