package com.queatz.ailaai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.navigation.NavController
import com.queatz.db.Person

data class AppState(
    val me: Person?,
    val navController: NavController? = null
)

val LocalAppState = compositionLocalOf<AppState>(
    neverEqualPolicy()
) {
    error("No state")
}

val me @Composable get() = LocalAppState.current.me

val nav @Composable get() = LocalAppState.current.navController ?: error("No NavController present")
