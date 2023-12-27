package com.queatz.ailaai.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
fun ResumeEffect(block: suspend () -> Unit) {
    LifecycleEffect {
        if (it == Lifecycle.Event.ON_RESUME) {
            block()
        }
    }
}

@Composable
fun StartEffect(block: suspend () -> Unit) {
    LifecycleEffect {
        if (it == Lifecycle.Event.ON_START) {
            block()
        }
    }
}
