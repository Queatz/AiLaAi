package com.queatz.ailaai.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import com.queatz.ailaai.R
import com.queatz.ailaai.cache.CacheKey
import com.queatz.ailaai.cache.CachedGroupEntry
import com.queatz.ailaai.cache.ClearCacheFailure
import com.queatz.ailaai.cache.cache
import com.queatz.ailaai.cache.groupMessageCache
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.LoadingIcon
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class ClearCacheFeedback {
    data object None : ClearCacheFeedback()
    data class Success(
        val clearedCount: Int,
        val freedBytes: Long,
    ) : ClearCacheFeedback()

    data class PartialSuccess(
        val clearedCount: Int,
        val freedBytes: Long,
        val failures: List<ClearCacheFailure>,
    ) : ClearCacheFeedback()

    data class Error(
        val message: String,
        val failures: List<ClearCacheFailure> = emptyList(),
    ) : ClearCacheFeedback()
}

private const val IMAGE_CACHE_ITEM_ID = "__image_cache__"

@Composable
fun ClearCacheScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = me
    val cacheEnabled by groupMessageCache.enabled.collectAsState()

    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val cachedImagesLabel = stringResource(R.string.cached_images)

    var entries by remember { mutableStateOf<List<CachedGroupEntry>>(emptyList()) }
    var imageCacheBytes by rememberStateOf(0L)
    var cachedGroupsLookup by remember { mutableStateOf<Map<String, GroupExtended>>(emptyMap()) }
    var selectedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clearImagesSelected by rememberStateOf(false)
    var isLoading by rememberStateOf(true)
    var isClearing by rememberStateOf(false)
    var loadError by rememberStateOf<String?>(null)
    var feedback by remember { mutableStateOf<ClearCacheFeedback>(ClearCacheFeedback.None) }

    fun resolveGroupName(entry: CachedGroupEntry): String {
        val group = entry.group ?: cachedGroupsLookup[entry.groupId]
        return group?.name(
            someone = someone,
            emptyGroup = emptyGroup,
            omit = me?.id?.inList().orEmpty(),
        ) ?: emptyGroup
    }

    suspend fun loadCacheEntries() {
        isLoading = true
        loadError = null

        val lookup = withContext(Dispatchers.IO) {
            cache.get<List<GroupExtended>>(CacheKey.Groups)
                ?.mapNotNull { group -> group.group?.id?.let { id -> id to group } }
                ?.toMap()
                .orEmpty()
        }
        cachedGroupsLookup = lookup

        val imageSize = withContext(Dispatchers.IO) {
            runCatching {
                context.imageLoader.diskCache?.size ?: 0L
            }.getOrDefault(0L)
        }
        imageCacheBytes = imageSize

        groupMessageCache.listEntries()
            .onSuccess { loaded ->
                entries = loaded
                val validIds = loaded.map { it.groupId }.toSet()
                selectedGroupIds = selectedGroupIds.intersect(validIds)
                if (imageSize <= 0L) {
                    clearImagesSelected = false
                }
            }
            .onFailure { error ->
                loadError = context.getString(
                    R.string.error_load_cache,
                    error.localizedMessage ?: error.javaClass.simpleName,
                )
            }

        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadCacheEntries()
    }

    val totalCacheBytes = remember(entries, imageCacheBytes) {
        entries.sumOf { it.sizeBytes } + imageCacheBytes
    }

    val totalItemCount = remember(entries, imageCacheBytes) {
        entries.size + if (imageCacheBytes > 0L) 1 else 0
    }

    val selectedCount = remember(selectedGroupIds, clearImagesSelected, imageCacheBytes) {
        selectedGroupIds.size + if (clearImagesSelected && imageCacheBytes > 0L) 1 else 0
    }

    val selectedBytes = remember(entries, selectedGroupIds, clearImagesSelected, imageCacheBytes) {
        entries
            .filter { it.groupId in selectedGroupIds }
            .sumOf { it.sizeBytes } + if (clearImagesSelected && imageCacheBytes > 0L) imageCacheBytes else 0L
    }

    val allSelected = totalItemCount > 0 && selectedCount == totalItemCount

    fun clearSelected() {
        if (selectedCount == 0 || isClearing) {
            return
        }

        scope.launch {
            isClearing = true
            feedback = ClearCacheFeedback.None

            val groupIdsToClear = selectedGroupIds.toList()
            val shouldClearImages = clearImagesSelected && imageCacheBytes > 0L
            val currentImageBytes = imageCacheBytes
            val entryNamesById = entries.associate { it.groupId to resolveGroupName(it) }

            val groupResult = if (groupIdsToClear.isNotEmpty()) {
                groupMessageCache.removeMultiple(
                    groupIds = groupIdsToClear,
                    resolveName = { id -> entryNamesById[id] ?: emptyGroup },
                )
            } else {
                null
            }

            var totalCleared = groupResult?.clearedCount ?: 0
            var totalFreed = groupResult?.freedBytes ?: 0L
            val allFailures = mutableListOf<ClearCacheFailure>().apply {
                groupResult?.failures?.let(::addAll)
            }

            if (shouldClearImages) {
                val imageClearResult = withContext(Dispatchers.IO) {
                    runCatching {
                        context.imageLoader.diskCache?.clear()
                        context.imageLoader.memoryCache?.clear()
                    }
                }
                imageClearResult
                    .onSuccess {
                        totalCleared++
                        totalFreed += currentImageBytes
                    }
                    .onFailure { error ->
                        allFailures += ClearCacheFailure(
                            id = IMAGE_CACHE_ITEM_ID,
                            name = cachedImagesLabel,
                            reason = error.localizedMessage ?: error.javaClass.simpleName,
                        )
                    }
            }

            selectedGroupIds = emptySet()
            clearImagesSelected = false
            loadCacheEntries()
            isClearing = false

            val formattedFreed = Formatter.formatShortFileSize(context, totalFreed)
            feedback = when {
                allFailures.isEmpty() -> {
                    context.toast(
                        context.resources.getQuantityString(
                            R.plurals.cache_cleared_success,
                            totalCleared,
                            totalCleared,
                            formattedFreed,
                        )
                    )
                    ClearCacheFeedback.Success(
                        clearedCount = totalCleared,
                        freedBytes = totalFreed,
                    )
                }

                totalCleared > 0 -> {
                    ClearCacheFeedback.PartialSuccess(
                        clearedCount = totalCleared,
                        freedBytes = totalFreed,
                        failures = allFailures,
                    )
                }

                else -> {
                    ClearCacheFeedback.Error(
                        message = context.getString(R.string.error_clear_cache_failed),
                        failures = allFailures,
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.clear_cache),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!isLoading) {
                        Text(
                            text = if (totalCacheBytes > 0L) {
                                stringResource(
                                    R.string.cache_total_x,
                                    Formatter.formatShortFileSize(context, totalCacheBytes),
                                )
                            } else {
                                stringResource(R.string.clear_cache_empty)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            },
            navigationIcon = {
                BackButton()
            },
            actions = {
                if (totalItemCount > 0) {
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                selectedGroupIds = emptySet()
                                clearImagesSelected = false
                            } else {
                                selectedGroupIds = entries.map { it.groupId }.toSet()
                                clearImagesSelected = imageCacheBytes > 0L
                            }
                        },
                        enabled = !isClearing && !isLoading,
                    ) {
                        Text(
                            text = stringResource(
                                if (allSelected) R.string.deselect_all else R.string.select_all
                            )
                        )
                    }
                }
            }
        )

        if (isLoading || isClearing) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(visible = loadError != null) {
            loadError?.let { errorText ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.pad, vertical = 1.pad)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(1.5f.pad)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                        )
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.pad)
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    loadCacheEntries()
                                }
                            },
                            enabled = !isLoading,
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = feedback !is ClearCacheFeedback.None) {
            ClearCacheFeedbackBanner(
                feedback = feedback,
                onDismiss = {
                    feedback = ClearCacheFeedback.None
                },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading && totalItemCount == 0 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.pad)
                    ) {
                        LoadingIcon()
                        Text(
                            text = stringResource(R.string.loading_cache_items),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                totalItemCount == 0 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.pad)
                    ) {
                        EmptyText(stringResource(R.string.cache_is_empty))
                        if (!cacheEnabled) {
                            Text(
                                text = stringResource(R.string.cache_disabled_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 2.pad)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 1.pad),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (imageCacheBytes > 0L) {
                            item(key = IMAGE_CACHE_ITEM_ID) {
                                CacheChecklistItem(
                                    title = cachedImagesLabel,
                                    subtitle = stringResource(R.string.cached_images_description),
                                    sizeText = Formatter.formatShortFileSize(context, imageCacheBytes),
                                    checked = clearImagesSelected,
                                    enabled = !isClearing,
                                    onToggle = {
                                        clearImagesSelected = !clearImagesSelected
                                    },
                                )
                            }
                            if (entries.isNotEmpty()) {
                                item(key = "__divider__") {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = .5f.pad)
                                    )
                                }
                            }
                        }

                        items(
                            items = entries,
                            key = { it.groupId },
                        ) { entry ->
                            val isChecked = entry.groupId in selectedGroupIds
                            CacheChecklistItem(
                                title = resolveGroupName(entry),
                                subtitle = pluralStringResource(
                                    R.plurals.cached_messages_count,
                                    entry.messageCount,
                                    entry.messageCount,
                                ),
                                sizeText = Formatter.formatShortFileSize(context, entry.sizeBytes),
                                checked = isChecked,
                                enabled = !isClearing,
                                onToggle = {
                                    selectedGroupIds = if (isChecked) {
                                        selectedGroupIds - entry.groupId
                                    } else {
                                        selectedGroupIds + entry.groupId
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.pad, vertical = 1.5f.pad)
            ) {
                if (selectedCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.cache_selected_summary,
                            selectedCount,
                            Formatter.formatShortFileSize(context, selectedBytes),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Button(
                    onClick = {
                        clearSelected()
                    },
                    enabled = selectedCount > 0 && !isClearing && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isClearing) {
                        LoadingIcon()
                        Spacer(modifier = Modifier.width(1.pad))
                        Text(stringResource(R.string.clearing_cache))
                    } else if (selectedCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.clear_selected_size,
                                Formatter.formatShortFileSize(context, selectedBytes),
                            )
                        )
                    } else {
                        Text(stringResource(R.string.clear_selected))
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheChecklistItem(
    title: String,
    subtitle: String,
    sizeText: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onToggle()
            }
            .padding(horizontal = 1.5f.pad, vertical = 1.pad)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                onToggle()
            },
            enabled = enabled,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 1.pad)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = sizeText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = .5f.pad)
        )
    }
}

@Composable
private fun ClearCacheFeedbackBanner(
    feedback: ClearCacheFeedback,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    when (feedback) {
        ClearCacheFeedback.None -> Unit

        is ClearCacheFeedback.Success -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.pad, vertical = 1.pad)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 1.5f.pad, top = 1.pad, bottom = 1.pad, end = .5f.pad)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.cache_cleared_success,
                            feedback.clearedCount,
                            feedback.clearedCount,
                            Formatter.formatShortFileSize(context, feedback.freedBytes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.pad)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                }
            }
        }

        is ClearCacheFeedback.PartialSuccess -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.pad, vertical = 1.pad)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(.5f.pad),
                    modifier = Modifier.padding(1.5f.pad)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(
                                R.string.error_clear_cache_partial,
                                pluralStringResource(
                                    R.plurals.x_items_cleared,
                                    feedback.clearedCount,
                                    feedback.clearedCount,
                                ),
                                Formatter.formatShortFileSize(context, feedback.freedBytes),
                                feedback.failures.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.pad)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    }
                    feedback.failures.forEach { failure ->
                        Text(
                            text = "• ${failure.name}: ${failure.reason}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        is ClearCacheFeedback.Error -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.pad, vertical = 1.pad)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(.5f.pad),
                    modifier = Modifier.padding(1.5f.pad)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                        )
                        Text(
                            text = feedback.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.pad)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    }
                    feedback.failures.forEach { failure ->
                        Text(
                            text = "• ${failure.name}: ${failure.reason}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
