package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Person
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun MessagesScreen(navController: NavController, me: () -> Person?) {
    var isLoading by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf(listOf<GroupExtended>()) }

    LaunchedEffect(true) {
        isLoading = true
        try {
            groups = api.groups().reversed()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isLoading = false
    }

    LazyColumn(
        contentPadding = PaddingValues(PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
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
                    "You currently have no messages.",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(PaddingDefault * 2)
                )
            }
        }

        items(groups, key = { it.group!!.id!! }) {
            ContactItem(navController, it, me())
        }
    }
}
