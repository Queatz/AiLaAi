package com.queatz.ailaai.ui.story.contents

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
import androidx.compose.foundation.text.selection.DisableSelection
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
import com.queatz.ailaai.ui.script.ScriptContent
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.story.Stub
import com.queatz.ailaai.ui.story.stringResource
import com.queatz.db.StoryContent
import com.queatz.db.Widget
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.WebData
import widget

@OptIn(ExperimentalComposeUiApi::class)
fun LazyGridScope.widgetItem(
    content: StoryContent.Widget,
    source: StorySource?,
    fullscreenWebView: (Pair<View, WebChromeClient.CustomViewCallback>?) -> Unit,
    showOpenWidgetDialog: (Boolean) -> Unit
) {
    when (content.widget) {
        Widgets.Script -> {
            ScriptContent(content.id)
        }
        Widgets.Web -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                var widget by remember(content.id) {
                    mutableStateOf<Widget?>(null)
                }

                var data by remember(content.id) {
                    mutableStateOf<WebData?>(null)
                }
                LaunchedEffect(content.id) {
                    // todo loading
                    api.widget(content.id) {
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
                                            view: View,
                                            callback: CustomViewCallback
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
                            }, update = {
                                it.loadUrl(url)
                                webView = it
                            }
                        )
                    }
                }
            }
        }
        else -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DisableSelection {
                    Stub(content.widget.stringResource) {
                        if (source is StorySource.Card) {
                            showOpenWidgetDialog(true)
                        }
                    }
                }
            }
        }
    }
}
