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
    var cards by rememberSaveable(stateSaver = gsonSaver<List<Card>>()) { mutableStateOf(listOf()) }
    var isLoading by remember { mutableStateOf(cards.isEmpty()) }
    var hasInitialCards by remember { mutableStateOf(cards.isNotEmpty()) }

    LaunchedEffect(Unit) {
        saves.reload()
    }

    LaunchedEffect(value) {
        if (hasInitialCards) {
            hasInitialCards = false

            if (cards.isNotEmpty()) {
                return@LaunchedEffect
            }
        }
        try {
            isLoading = true
            cards = api.savedCards(value.takeIf { it.isNotBlank() }).mapNotNull { it.card }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    CardsList(cards, isLoading, value, { value = it}, navController)
}
