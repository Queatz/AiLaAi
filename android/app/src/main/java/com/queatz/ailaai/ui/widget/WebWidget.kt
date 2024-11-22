package com.queatz.ailaai.ui.widget

import android.view.MotionEvent
import android.view.View
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
import androidx.compose.runtime.LaunchedEffect
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
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.db.Widget
import com.queatz.widgets.widgets.WebData
import widget


@OptIn(ExperimentalComposeUiApi::class)
fun LazyGridScope.WebWidgetContent(
    widgetId: String,
    fullscreenWebView: (Pair<View, WebChromeClient.CustomViewCallback>?) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        var widget by remember(widgetId) {
            mutableStateOf<Widget?>(null)
        }

        var data by remember(widgetId) {
            mutableStateOf<WebData?>(null)
        }
        LaunchedEffect(widgetId) {
            // todo loading
            api.widget(widgetId) {
                it.data ?: return@widget
                widget = it
                data = json.decodeFromString<WebData>(it.data!!)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))

        ) {
            data?.url?.let { url ->
                val disallowIntercept = remember {
                    RequestDisallowInterceptTouchEvent()
                }
                var webView by remember { mutableStateOf<WebView?>(null) }

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
                                MotionEvent.ACTION_UP,
                                    -> {
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
                                    view: View,
                                    callback: CustomViewCallback,
                                ) {
                                    fullscreenWebView(view to callback)
                                }

                                override fun onHideCustomView() {
                                    fullscreenWebView(null)
                                }
                            }

                            loadUrl(url)
                            webView = this
                        }
                    },
                    update = {
                        it.loadUrl(url)
                        webView = it
                    }
                )
            }
        }
    }
}
