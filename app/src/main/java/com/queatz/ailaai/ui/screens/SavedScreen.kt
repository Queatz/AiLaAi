package com.queatz.ailaai.ui.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import com.queatz.ailaai.Card
import com.queatz.ailaai.Person
import com.queatz.ailaai.api
import com.queatz.ailaai.saves
import com.queatz.ailaai.ui.components.CardsList
import com.queatz.ailaai.ui.state.gsonSaver

@Composable
fun SavedScreen(context: Context, navController: NavController, me: () -> Person?) {
    var value by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var cards by rememberSaveable(stateSaver = gsonSaver<List<Card>>()) { mutableStateOf(listOf()) }
    var hasInitialCards by remember { mutableStateOf(cards.isNotEmpty()) }

    LaunchedEffect(Unit) {
        saves.reload()
    }

    // The LaunchedEffect below could have lag and allow isLoading to initially be false
    if (!hasInitialCards) {
        isLoading = cards.isEmpty()
    }

    LaunchedEffect(value) {
        if (hasInitialCards) {
            hasInitialCards = false

            if (cards.isNotEmpty()) {
                return@LaunchedEffect
            }
        }
        isLoading = true
        try {
            cards = api.savedCards(value.takeIf { it.isNotBlank() }).mapNotNull { it.card }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isLoading = false
    }

    CardsList(cards, isLoading, value, { value = it}, navController)
}
