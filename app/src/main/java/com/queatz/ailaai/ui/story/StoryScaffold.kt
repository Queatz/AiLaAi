package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun StoryScaffold(
    goBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
                .padding(top = PaddingDefault)
        ) {
            IconButton(
                {
                    goBack()
                }
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    stringResource(R.string.go_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            actions()
        }
        content()
    }
}
