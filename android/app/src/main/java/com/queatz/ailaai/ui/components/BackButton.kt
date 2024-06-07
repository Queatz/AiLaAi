package com.queatz.ailaai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.nav

@Composable
fun BackButton() {
    val nav = nav

    IconButton({
        nav.popBackStackOrFinish()
    }) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
    }
}
