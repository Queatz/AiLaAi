package com.queatz.ailaai.ui.story

import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.idOrUrl
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.shareAsUrl
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.story.contents.audioItem
import com.queatz.ailaai.ui.story.contents.authorsItem
import com.queatz.ailaai.ui.story.contents.buttonItem
import com.queatz.ailaai.ui.story.contents.cardsItem
import com.queatz.ailaai.ui.story.contents.commentsItem
import com.queatz.ailaai.ui.story.contents.dividerItem
import com.queatz.ailaai.ui.story.contents.groupsItem
import com.queatz.ailaai.ui.story.contents.inputItem
import com.queatz.ailaai.ui.story.contents.photosItem
import com.queatz.ailaai.ui.story.contents.profilesItem
import com.queatz.ailaai.ui.story.contents.reactionsItem
import com.queatz.ailaai.ui.story.contents.sectionItem
import com.queatz.ailaai.ui.story.contents.textItem
import com.queatz.ailaai.ui.story.contents.titleItem
import com.queatz.ailaai.ui.story.contents.widgetItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.StoryContent

@Composable
fun StoryContents(
    source: StorySource?,
    content: List<StoryContent>,
    state: LazyGridState,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.pad,
    horizontalPadding: Dp = 2.pad,
    fade: Boolean = false,
    onReloadRequest: () -> Unit = {},
    onCommentFocused: (Boolean) -> Unit = {},
    header: LazyGridScope.() -> Unit = {},
    onButtonClick: ((script: String, data: String?, input: Map<String, String?>) -> Unit)? = null,
    actions: (@Composable (storyId: String) -> Unit)? = null,
) {
    var viewHeight by rememberStateOf(Float.MAX_VALUE)
    var showOpenWidgetDialog by rememberStateOf(false)
    var size by rememberStateOf(Size.Zero)
    val me = me
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var fullscreenWebView by remember {
        mutableStateOf<Pair<View, WebChromeClient.CustomViewCallback>?>(null)
    }
    var input by remember(content) {
        mutableStateOf(
            buildMap<String, String?> {
                content.filterIsInstance<StoryContent.Input>().forEach {
                    put(it.key, it.value)
                }
            }
        )
    }

    fullscreenWebView?.let { (view, closeCallback) ->
        DialogBase({
            closeCallback.onCustomViewHidden()
        }) {
            DialogLayout(
                scrollable = false,
                padding = 2.pad,
                content = {
                    AndroidView(
                        factory = {
                            view
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.medium)
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            closeCallback.onCustomViewHidden()
                        },
                        modifier = Modifier
                            .padding(top = 1.pad)
                    ) {
                        Text(stringResource(R.string.go_back))
                    }
                }
            )
        }
    }

    if (showOpenWidgetDialog) {
        AlertDialog(
            onDismissRequest = {
                showOpenWidgetDialog = false
            },
            text = {
                Text(stringResource(R.string.widget_web_only))
            },
            dismissButton = {
                DialogCloseButton {
                    showOpenWidgetDialog = false
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOpenWidgetDialog = false
                        Card().apply {
                            id = (source as StorySource.Card).id
                        }.idOrUrl.shareAsUrl(context = context, name = null)
                    }
                ) {
                    Text(stringResource(R.string.open_card))
                }
            }
        )
    }

    SelectionContainer(modifier = modifier) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                horizontalPadding,
                0.dp,
                horizontalPadding,
                2.pad + bottomContentPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
            modifier = Modifier
                .onPlaced {
                    size = it.boundsInParent().size
                    viewHeight = it.boundsInParent().height
                }
                .let {
                    if (fade) {
                        it.fadingEdge(size, state, 12f)
                    } else {
                        it
                    }
                }
        ) {
            header()
            content.forEach { content ->
                when (content) {
                    is StoryContent.Divider -> dividerItem()

                    is StoryContent.Comments -> commentsItem(content, onCommentFocused)

                    is StoryContent.Reactions -> reactionsItem(
                        content = content,
                        context = context,
                        scope = scope,
                        me = me,
                        onCommentFocused = onCommentFocused,
                        onReactionChange = onReloadRequest
                    )

                    is StoryContent.Title -> titleItem(content, actions)

                    is StoryContent.Section -> sectionItem(content)

                    is StoryContent.Text -> textItem(content)

                    is StoryContent.Authors -> authorsItem(content)

                    is StoryContent.Groups -> groupsItem(content)

                    is StoryContent.Cards -> cardsItem(content, viewHeight)

                    is StoryContent.Photos -> photosItem(content, viewHeight)

                    is StoryContent.Audio -> audioItem(content)

                    is StoryContent.Widget -> widgetItem(
                        content = content,
                        source = source,
                        fullscreenWebView = { fullscreenWebView = it },
                        showOpenWidgetDialog = { showOpenWidgetDialog = it }
                    )

                    is StoryContent.Button -> buttonItem(
                        content = content,
                        onButtonClick = { script: String, data: String? ->
                            onButtonClick?.invoke(script, data, input)
                        }
                    )

                    is StoryContent.Input -> {
                        inputItem(
                            content = content,
                            onValueChange = {
                                input = input + (content.key to it)
                            }
                        )
                    }

                    is StoryContent.Profiles -> profilesItem(content)
                }
            }
        }
    }
}
