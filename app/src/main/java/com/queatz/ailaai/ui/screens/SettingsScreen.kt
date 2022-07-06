package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.em
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun SettingsScreen(navController: NavController, me: Person?) {
    Column {
        SmallTopAppBar(
            {
                Text("Settings")
            },
            navigationIcon = {
                IconButton({
                    navController.popBackStack()
                }) {
                    Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                }
            }
        )
        DropdownMenuItem({
            Column(modifier = Modifier.padding(PaddingDefault)) {
                Text(
                    "Language",
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                )
                Text(
                    "English",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }, {})
        DropdownMenuItem({
            Text(
                "Sign out",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(PaddingDefault)
            )
        }, {})
    }
}
