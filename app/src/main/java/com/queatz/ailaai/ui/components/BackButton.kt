package com.queatz.ailaai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.popBackStackOrFinish

@Composable
fun BackButton(navController: NavController) {
    IconButton({
        navController.popBackStackOrFinish()
    }) {
        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.go_back))
    }
}
