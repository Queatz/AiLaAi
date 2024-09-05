package com.queatz.ailaai.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
inline fun <T> rememberStateOf(initialState: T) = remember {
    mutableStateOf(initialState)
}

@Composable
inline fun <T> rememberSavableStateOf(initialState: T) = rememberSaveable {
    mutableStateOf(initialState)
}
