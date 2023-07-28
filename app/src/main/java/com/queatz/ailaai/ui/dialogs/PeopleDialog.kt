package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.PersonMember
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun PeopleDialog(
    title: String,
    onDismissRequest: () -> Unit,
    people: List<Person>,
    showCountInTitle: Boolean = true,
    infoFormatter: (Person) -> String? = { null },
    extraButtons: @Composable RowScope.() -> Unit = {},
    onClick: (Person) -> Unit,
) {
    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(PaddingDefault * 3)
        ) {
            Text(
                if (showCountInTitle) "$title (${people.size})" else title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                items(people, key = { it.id!! }) {
                    PersonMember(it, infoFormatter = infoFormatter) { onClick(it) }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                extraButtons()
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
