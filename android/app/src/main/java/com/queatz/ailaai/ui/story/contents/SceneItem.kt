package com.queatz.ailaai.ui.story.contents

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.queatz.db.StoryContent

@OptIn(ExperimentalComposeUiApi::class)
fun LazyGridScope.sceneItem(
    content: StoryContent.Scene,
    fullscreenWebView: (Pair<android.view.View, WebChromeClient.CustomViewCallback>?) -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            val sceneUrl = "https://ailaai.app/scene/${content.sceneId}"
            var webView by remember { mutableStateOf<WebView?>(null) }
            val disallowIntercept = remember {
                RequestDisallowInterceptTouchEvent()
            }

            AndroidView(
                modifier = Modifier
                    .pointerInteropFilter(disallowIntercept) { event ->
                        webView?.dispatchTouchEvent(event)
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                disallowIntercept(true)
                                true
                            }
                            MotionEvent.ACTION_CANCEL,
                            MotionEvent.ACTION_UP -> {
                                disallowIntercept(false)
                                true
                            }
                            else -> true
                        }
                    },
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowContentAccess = true

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(
                                view: android.view.View,
                                callback: CustomViewCallback,
                            ) {
                                fullscreenWebView(view to callback)
                            }

                            override fun onHideCustomView() {
                                fullscreenWebView(null)
                            }
                        }

                        loadUrl(sceneUrl)
                        webView = this
                    }
                },
                update = {
                    it.loadUrl(sceneUrl)
                    webView = it
                }
            )
        }
    }
}
