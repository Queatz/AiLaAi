package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.PersonMember
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun GroupMembersDialog(onDismissRequest: () -> Unit, people: List<Person>, infoFormatter: (Person) -> String? = { null }, onClick: (Person) -> Unit) {
    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault * 3)
            ) {
                LazyColumn {
                    items(people, key = { it.id!! }) {
                        PersonMember(it, infoFormatter = infoFormatter) { onClick(it) }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        {
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}
