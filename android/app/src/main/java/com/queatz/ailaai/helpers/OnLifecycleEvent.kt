package com.queatz.ailaai.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

@Composable
fun LifecycleEffect(onEvent: suspend (event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            scope.launch {
                eventHandler.value(event)
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun ResumeEffect(skipFirst: Boolean = false, block: suspend () -> Unit) {
    var isFirst by remember(skipFirst) { mutableStateOf(true) }
    LifecycleEffect {
        if (it == Lifecycle.Event.ON_RESUME) {
            if (skipFirst && isFirst) {
                isFirst = false
                return@LifecycleEffect
            }
            block()
        }
    }
}

@Composable
fun StartEffect(skipFirst: Boolean = false, block: suspend () -> Unit) {
    var isFirst by remember(skipFirst) { mutableStateOf(true) }
    LifecycleEffect {
        if (it == Lifecycle.Event.ON_START) {
            if (skipFirst && isFirst) {
                isFirst = false
                return@LifecycleEffect
            }
            block()
        }
    }
}

@Composable
fun StopEffect(skipFirst: Boolean = false, block: suspend () -> Unit) {
    var isFirst by remember(skipFirst) { mutableStateOf(true) }
    LifecycleEffect {
        if (it == Lifecycle.Event.ON_STOP) {
            if (skipFirst && isFirst) {
                isFirst = false
                return@LifecycleEffect
            }
            block()
        }
    }
}
