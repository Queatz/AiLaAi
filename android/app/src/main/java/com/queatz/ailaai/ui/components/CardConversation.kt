package com.queatz.ailaai.ui.components

import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import at.bluesource.choicesdk.maps.common.LatLng
import app.ailaai.api.saveCard
import app.ailaai.api.unsaveCard
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.approximate
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.hint
import com.queatz.ailaai.extensions.latLng
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Activity
import com.queatz.db.Card
import com.queatz.db.Person
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardConversation(
    card: Card,
    modifier: Modifier = Modifier,
    interactable: Boolean = true,
    onReply: (List<String>) -> Unit = {},
    onTitleClick: () -> Unit = {},
    showDistance: LatLng? = null,
    showTitle: Boolean = true,
    largeTitle: Boolean = false,
    showAuthors: Boolean = true,
    hideCreators: List<String>? = null,
    selectingText: ((Boolean) -> Unit)? = null,
    conversationChange: ((List<ConversationItem>) -> Unit)? = null
) {
    val conversation = remember(card.conversation) {
        json.decodeFromString<ConversationItem>(card.conversation ?: "{}")
    }
    val options = remember(card.options) {
        json.decodeFromString<CardOptions>(card.options ?: "{}")
    }
    var current by remember(conversation) { mutableStateOf(conversation) }
    var stack by remember(conversation) { mutableStateOf(emptyList<ConversationItem>()) }

    LaunchedEffect(stack) {
        conversationChange?.invoke(stack)
    }

    fun people() = (listOf(card.person!!) + (card.collaborators ?: emptyList()))

    var cardAuthors by rememberStateOf<List<Person>?>(
        people().mapNotNull {
            authors.cached(it)
        }
    )

    LaunchedEffect(Unit) {
        cardAuthors = people().mapNotNull {
            authors.person(it)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
    ) {
        val hasTitle = showTitle || stack.isNotEmpty()
        Column {
            if (hasTitle) {
                val text = buildAnnotatedString {
                    withStyle(
                        if (largeTitle) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.titleMedium
                        }.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                    ) {
                        append(current.title.takeIf { stack.isNotEmpty() } ?: card.name ?: "")
                    }

                    val hint = card.name?.takeIf { stack.size == 1 } ?: stack.lastOrNull()?.title ?: card.hint

                    if (hint.isNotBlank()) {
                        withStyle(
                            if (largeTitle) {
                                MaterialTheme.typography.headlineSmall
                            } else {
                                MaterialTheme.typography.titleSmall
                            }.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append("  $hint")
                        }
                    }
                }
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = if (largeTitle) {
                                MaterialTheme.typography.headlineMedium.lineHeight
                            } else {
                                MaterialTheme.typography.titleMedium.lineHeight
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(remember { MutableInteractionSource() }, null) {
                                onTitleClick()
                            }
                    )
                }

                if (stack.isEmpty()) {
                    val hasCards = (card.cardCount ?: 0) > 0
                    val distanceText = showDistance?.let {
                        if (card.geo != null) {
                            it.distance(card.latLng).let { metersAway ->
                                when {
                                    metersAway >= 1000f -> ceil(metersAway / 1000f).toInt()
                                        .let { km -> pluralStringResource(R.plurals.km_away, km, km.format()) }

                                    else -> metersAway.approximate(10)
                                        .let { meters -> pluralStringResource(R.plurals.meters_away, meters, meters.format()) }
                                } + (if (hasCards) ", " else "")
                            }
                        } else {
                            ""
                        }
                    }

                    val hint = (distanceText ?: "") + if (hasCards) pluralStringResource(
                        R.plurals.number_of_cards,
                        card.cardCount ?: 0,
                        card.cardCount ?: 0
                    ) else ""

                    if (hint.isNotBlank()) {
                        Text(
                            hint,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (stack.isEmpty() && showAuthors) {
            cardAuthors?.takeIf {
                hideCreators == null || it.any { it.id!! !in hideCreators }
            }?.let { authors ->
                CardAuthor(
                    authors,
                    interactable = interactable
                )
            }
        }

        card.activity?.let {
            CardActivity(it)
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var isSaving by remember { mutableStateOf(false) }
        var isSaved by remember(card.saved) { mutableStateOf(card.saved == true) }

        Toolbar(
            singleLine = true,
            modifier = Modifier.padding(top = 1.pad)
        ) {
            card.geo?.let { geo ->
                if (geo.size >= 2) {
                    item(
                        icon = Icons.Outlined.Directions,
                        name = stringResource(R.string.directions),
                        onClick = {
                            val lat = geo[0]
                            val lng = geo[1]
                            val name = card.name ?: ""
                            val uri = Uri.parse("geo:0,0?q=$lat,$lng($name)")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    )
                }
            }
            item(
                icon = Icons.Outlined.Share,
                name = stringResource(R.string.share),
                onClick = {
                    val url = "$appDomain/page/${card.id ?: card.url ?: ""}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            )
            item(
                icon = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                name = if (isSaved) stringResource(R.string.saved) else stringResource(R.string.save),
                isLoading = isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        val success = if (isSaved) {
                            var ok = false
                            api.unsaveCard(card.id!!) { ok = true }
                            ok
                        } else {
                            var ok = false
                            api.saveCard(card.id!!) { ok = true }
                            ok
                        }
                        if (success) {
                            isSaved = !isSaved
                            card.saved = isSaved
                        }
                        isSaving = false
                    }
                }
            )
        }

        // todo: card tools

        if (current.message.isNotBlank()) {
            SelectionContainer(
                modifier = Modifier
                    .motionEventSpy {
                        if (it.action == MotionEvent.ACTION_UP || it.action == MotionEvent.ACTION_CANCEL) {
                            selectingText?.invoke(false)
                        } else if (it.action != MotionEvent.ACTION_MOVE && it.action != MotionEvent.ACTION_DOWN) {
                            selectingText?.invoke(false)
                        }
                    }
                    .pointerInteropFilter {
                        if (it.action == MotionEvent.ACTION_DOWN) {
                            selectingText?.invoke(true)
                        }
                        false
                    }
            ) {
                LinkifyText(
                    text = current.message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        if (interactable) {
            Column {
                current.items.forEach {
                    when (it.action) {
                        ConversationAction.Message -> {
                            Button({
                                onReply(stack.map { it.title } + current.title + it.title)
                            }) {
                                Icon(Icons.AutoMirrored.Outlined.Message, "", modifier = Modifier.padding(end = 1.pad))
                                Text(it.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                        }

                        else -> {
                            OutlinedButton({
                                stack = stack + current
                                current = it
                            }) {
                                Text(it.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                        }
                    }
                }

                if (current.items.isEmpty() && options.enableReplies != false) {
                    Button({
                        onReply(stack.map { it.title } + current.title)
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.Message, "", modifier = Modifier.padding(end = 1.pad))
                        Text(
                            stringResource(R.string.message),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }

                AnimatedVisibility(
                    stack.isNotEmpty(),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    TextButton({
                        if (stack.isNotEmpty()) {
                            current = stack.last()
                            stack = stack.dropLast(1)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.go_back))
                        Text(stringResource(R.string.go_back), modifier = Modifier.padding(start = 1.pad))
                    }
                }
            }
        }
    }
}
