package com.queatz.ailaai.ui.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.api.profile
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.approximate
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.latLng
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.math.ceil

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardConversation(
    card: Card,
    navController: NavController,
    modifier: Modifier = Modifier,
    interactable: Boolean = true,
    onReply: (List<String>) -> Unit = {},
    onTitleClick: () -> Unit = {},
    isMine: Boolean = false,
    showDistance: LatLng? = null,
    showTitle: Boolean = true,
    selectingText: ((Boolean) -> Unit)? = null,
    conversationChange: ((List<ConversationItem>) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    onSetCategoryClick: (() -> Unit)? = null
) {
    val categories = card.categories ?: emptyList()
    val conversation = remember(card.conversation) {
        json.decodeFromString<ConversationItem>(card.conversation ?: "{}")
    }
    var current by remember(conversation) { mutableStateOf(conversation) }
    var stack by remember(conversation) { mutableStateOf(emptyList<ConversationItem>()) }

    LaunchedEffect(stack) {
        conversationChange?.invoke(stack)
    }

    var cardAuthors by rememberStateOf<List<Person>?>(null)

    LaunchedEffect(Unit) {
        val people = mutableListOf<Person>()
        (listOf(card.person!!) + (card.collaborators ?: emptyList())).forEach {
            authors.person(it)?.let(people::add)
        }
        cardAuthors = people
    }

    Column(modifier = modifier) {
        if (showTitle || stack.isNotEmpty()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                    ) {
                        append(current.title.takeIf { stack.isNotEmpty() } ?: card.name ?: "")
                    }

                    append("  ")

                    withStyle(
                        MaterialTheme.typography.titleSmall.toSpanStyle()
                            .copy(color = MaterialTheme.colorScheme.secondary)
                    ) {
                        append(card.name?.takeIf { stack.size == 1 } ?: stack.lastOrNull()?.title ?: card.location
                        ?: "")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(MutableInteractionSource(), null) {
                        onTitleClick()
                    }
            )

            if (stack.isEmpty()) {
                val hasCards = (card.cardCount ?: 0) > 0
                val distanceText = showDistance?.let {
                    if (card.geo != null) {
                        it.distance(card.latLng!!).takeIf { it < nearbyMaxDistanceKm }?.let { metersAway ->
                            when {
                                metersAway >= 1000f -> ceil(metersAway / 1000).toInt()
                                    .let { km -> pluralStringResource(R.plurals.km_away, km, km) }

                                else -> metersAway.approximate(10)
                                    .let { meters -> pluralStringResource(R.plurals.meters_away, meters, meters) }
                            } + (if (hasCards) ", " else "")
                        } ?: (stringResource(R.string.your_friend) + (if (hasCards) ", " else ""))
                    } else {
                        stringResource(R.string.your_friend) + (if (hasCards) ", " else "")
                    }
                }

                if (hasCards || distanceText != null) {
                    Text(
                        (distanceText ?: "") + if (hasCards) pluralStringResource(
                            R.plurals.number_of_cards,
                            card.cardCount ?: 0,
                            card.cardCount ?: 0
                        ) else "",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        if (categories.isNotEmpty()) {
            AnimatedVisibility(stack.isEmpty()) {
                categories.forEach { category ->
                    SuggestionChip(
                        onClick = {
                            onCategoryClick?.invoke(category)
                        },
                        shape = MaterialTheme.shapes.medium,
                        label = {
                            Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }
            }
        } else if (isMine) {
            AnimatedVisibility(stack.isEmpty()) {
                AssistChip(
                    onClick = {
                        onSetCategoryClick?.invoke()
                    },
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, null)
                    },
                    label = {
                        Text(stringResource(R.string.set_category))
                    }
                )
            }
        }

        AnimatedVisibility(cardAuthors != null && stack.isEmpty()) {
            cardAuthors?.let { authors ->
                CardAuthor(authors, interactable = interactable, navController, modifier = Modifier.padding(
                    top = if ((categories.isNotEmpty() || isMine) && stack.isEmpty()) 0.dp else PaddingDefault,
                    bottom = PaddingDefault)
                )
            }
        }

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
                        .padding(bottom = PaddingDefault * 2)
                )
            }
        }

        if (interactable) {
            current.items.forEach {
                when (it.action) {
                    ConversationAction.Message -> {
                        Button({
                            onReply(stack.map { it.title } + current.title + it.title)
                        }, enabled = !isMine) {
                            Icon(Icons.Outlined.Message, "", modifier = Modifier.padding(end = PaddingDefault))
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

            if (current.items.isEmpty()) {
                Button({
                    onReply(stack.map { it.title } + current.title)
                }, enabled = !isMine) {
                    Icon(Icons.Outlined.Message, "", modifier = Modifier.padding(end = PaddingDefault))
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
                    Icon(Icons.Outlined.ArrowBack, stringResource(R.string.go_back))
                    Text(stringResource(R.string.go_back), modifier = Modifier.padding(start = PaddingDefault))
                }
            }
        }
    }
}
