package com.queatz.ailaai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.navigation.NavController
import com.queatz.db.Person

data class AppState(
    val me: Person? = null,
    val navController: NavController? = null,
    val apiIsReachable: Boolean = true,
    val exists: Boolean = true,
)

val LocalAppState = compositionLocalOf(
    neverEqualPolicy()
) {
    AppState(exists = false)
}

val me @Composable get() = LocalAppState.current.me

val nav @Composable get() = navOrNull ?: error("No NavController present")

val navOrNull: NavController? @Composable get() = LocalAppState.current.takeIf { it.exists }?.navController
