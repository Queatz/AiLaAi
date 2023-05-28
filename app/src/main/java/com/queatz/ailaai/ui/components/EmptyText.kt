package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(PaddingDefault * 2).fillMaxWidth().then(modifier)
    )
}
