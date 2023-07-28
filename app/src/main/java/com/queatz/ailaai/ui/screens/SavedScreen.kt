package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.savedCards
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.services.saves
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.CardList
import io.ktor.utils.io.*
import kotlinx.coroutines.launch

@Composable
fun SavedScreen(navController: NavController, me: () -> Person?) {
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var value by rememberSaveable { mutableStateOf("") }
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var isLoading by rememberStateOf(true)
    var isError by rememberStateOf(false)
    var hasInitialCards by remember { mutableStateOf(cards.isNotEmpty()) }

    LaunchedEffect(Unit) {
        saves.reload()
    }

    suspend fun reload() {
        isError = false
        api.savedCards(
            value.takeIf { it.isNotBlank() },
            onError = { ex ->
                if (ex is CancellationException || ex is InterruptedException) {
                    // Ignore, probably geo or search value changed, keep isLoading = true
                } else {
                    isLoading = false
                    isError = true
                }
            }) {
            cards = it.mapNotNull { it.card }
        }
        isLoading = false
    }

    LaunchedEffect(value) {
        if (hasInitialCards) {
            hasInitialCards = false

            if (cards.isNotEmpty()) {
                return@LaunchedEffect
            }
        }
        isLoading = true
        reload()
    }

    Column {
        AppHeader(
            navController,
            if (isLoading) {
                stringResource(R.string.saved)
            } else {
                "${stringResource(R.string.saved)} (${cards.size})"
            },
            {
                scope.launch {
                    state.scrollToTop()
                }
            },
            me
        )
        CardList(
            state = state,
            cards = cards,
            isMine = { it.person == me()?.id },
            onChanged = { scope.launch { reload() } },
            geo = null,
            isLoading = isLoading,
            isError = isError,
            value = value,
            valueChange = { value = it },
            navController = navController
        )
    }
}
