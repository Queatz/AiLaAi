package com.queatz.ailaai.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
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
import coil3.disk.DiskCache
import coil3.imageLoader
import coil3.memory.MemoryCache
import com.queatz.ailaai.R
import com.queatz.ailaai.cache.CacheKey
import com.queatz.ailaai.cache.CachedGroupEntry
import com.queatz.ailaai.cache.ClearCacheFailure
import com.queatz.ailaai.cache.cache
import com.queatz.ailaai.cache.collectGroupImageUrls
import com.queatz.ailaai.cache.groupMessageCache
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.LoadingIcon
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import java.io.File

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

private data class CachedImageRef(
    val url: String,
    val dataFileName: String,
    val sizeBytes: Long,
)

private data class GroupCacheItem(
    val entry: CachedGroupEntry,
    val cachedImages: List<CachedImageRef>,
) {
    val groupId: String get() = entry.groupId
    val messageCount: Int get() = entry.messageCount
    val messageSizeBytes: Long get() = entry.sizeBytes
    val imageCount: Int get() = cachedImages.size
    val imageSizeBytes: Long get() = cachedImages.sumOf { it.sizeBytes }
    val totalSizeBytes: Long get() = messageSizeBytes + imageSizeBytes
}

private data class LoadedImageCacheState(
    val groupItems: List<GroupCacheItem>,
    val totalImageCacheBytes: Long,
    val otherImageCount: Int,
    val otherImageBytes: Long,
)

private const val OTHER_IMAGES_ITEM_ID = "__other_images__"

@Composable
fun ClearCacheScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = me
    val cacheEnabled by groupMessageCache.enabled.collectAsState()

    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)
    val otherLabel = stringResource(R.string.other)
    val cachedImagesLabel = stringResource(R.string.cached_images)

    var groupItems by remember { mutableStateOf<List<GroupCacheItem>>(emptyList()) }
    var totalImageCacheBytes by rememberStateOf(0L)
    var otherImageCount by rememberStateOf(0)
    var otherImageBytes by rememberStateOf(0L)
    var cachedGroupsLookup by remember { mutableStateOf<Map<String, GroupExtended>>(emptyMap()) }
    var selectedMessageGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedImageGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clearOtherImagesSelected by rememberStateOf(false)
    var showSelectAllMenu by rememberStateOf(false)
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

        val entriesResult = groupMessageCache.listEntries()
        val loadedEntries = entriesResult.getOrElse { error ->
            loadError = context.getString(
                R.string.error_load_cache,
                error.localizedMessage ?: error.javaClass.simpleName,
            )
            emptyList()
        }

        val imageCacheState = withContext(Dispatchers.IO) {
            inspectImageCache(
                diskCache = context.imageLoader.diskCache,
                entries = loadedEntries,
                groupsLookup = lookup,
            )
        }

        groupItems = imageCacheState.groupItems
        totalImageCacheBytes = imageCacheState.totalImageCacheBytes
        otherImageCount = imageCacheState.otherImageCount
        otherImageBytes = imageCacheState.otherImageBytes

        val validMessageGroupIds = imageCacheState.groupItems
            .filter { it.messageSizeBytes > 0L }
            .map { it.groupId }
            .toSet()
        val validImageGroupIds = imageCacheState.groupItems
            .filter { it.cachedImages.isNotEmpty() }
            .map { it.groupId }
            .toSet()

        selectedMessageGroupIds = selectedMessageGroupIds.intersect(validMessageGroupIds)
        selectedImageGroupIds = selectedImageGroupIds.intersect(validImageGroupIds)
        if (imageCacheState.otherImageCount == 0 && imageCacheState.otherImageBytes <= 0L) {
            clearOtherImagesSelected = false
        }

        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadCacheEntries()
    }

    val hasOtherImages = otherImageCount > 0 || otherImageBytes > 0L

    val allMessageGroupIds = remember(groupItems) {
        groupItems.filter { it.messageSizeBytes > 0L }.map { it.groupId }.toSet()
    }

    val allImageGroupIds = remember(groupItems) {
        groupItems.filter { it.cachedImages.isNotEmpty() }.map { it.groupId }.toSet()
    }

    val hasAnyCachedMessages = allMessageGroupIds.isNotEmpty()
    val hasAnyCachedImages = allImageGroupIds.isNotEmpty() || hasOtherImages

    val totalCacheBytes = remember(groupItems, totalImageCacheBytes) {
        groupItems.sumOf { it.messageSizeBytes } + totalImageCacheBytes
    }

    val totalItemCount = remember(allMessageGroupIds, allImageGroupIds, hasOtherImages) {
        allMessageGroupIds.size + allImageGroupIds.size + if (hasOtherImages) 1 else 0
    }

    val selectedCount = remember(selectedMessageGroupIds, selectedImageGroupIds, clearOtherImagesSelected, hasOtherImages) {
        selectedMessageGroupIds.size +
            selectedImageGroupIds.size +
            if (clearOtherImagesSelected && hasOtherImages) 1 else 0
    }

    val clearingAllImages = remember(
        hasAnyCachedImages,
        selectedImageGroupIds,
        allImageGroupIds,
        hasOtherImages,
        clearOtherImagesSelected,
    ) {
        hasAnyCachedImages &&
            selectedImageGroupIds.containsAll(allImageGroupIds) &&
            (!hasOtherImages || clearOtherImagesSelected)
    }

    val selectedBytes = remember(
        groupItems,
        selectedMessageGroupIds,
        selectedImageGroupIds,
        clearOtherImagesSelected,
        hasOtherImages,
        clearingAllImages,
        totalImageCacheBytes,
        otherImageBytes,
    ) {
        val messageBytes = groupItems
            .filter { it.groupId in selectedMessageGroupIds }
            .sumOf { it.messageSizeBytes }

        val imageBytes = if (clearingAllImages) {
            totalImageCacheBytes
        } else {
            val uniqueGroupImageBytes = linkedMapOf<String, Long>()
            groupItems
                .filter { it.groupId in selectedImageGroupIds }
                .forEach { item ->
                    item.cachedImages.forEach { ref ->
                        uniqueGroupImageBytes.putIfAbsent(ref.dataFileName, ref.sizeBytes)
                    }
                }
            uniqueGroupImageBytes.values.sum() +
                if (clearOtherImagesSelected && hasOtherImages) otherImageBytes else 0L
        }

        messageBytes + imageBytes
    }

    fun clearSelected() {
        if (selectedCount == 0 || isClearing) {
            return
        }

        scope.launch {
            isClearing = true
            feedback = ClearCacheFeedback.None

            val messageGroupIdsToClear = selectedMessageGroupIds.toList()
            val imageGroupIdsToClear = selectedImageGroupIds.toSet()
            val shouldClearOtherImages = clearOtherImagesSelected && hasOtherImages
            val shouldClearAllImages = clearingAllImages
            val currentItems = groupItems
            val entryNamesById = currentItems.associate { it.groupId to resolveGroupName(it.entry) }

            val groupMessageResult = if (messageGroupIdsToClear.isNotEmpty()) {
                groupMessageCache.removeMultiple(
                    groupIds = messageGroupIdsToClear,
                    resolveName = { id -> entryNamesById[id] ?: emptyGroup },
                )
            } else {
                null
            }

            var totalCleared = groupMessageResult?.clearedCount ?: 0
            var totalFreed = groupMessageResult?.freedBytes ?: 0L
            val allFailures = mutableListOf<ClearCacheFailure>().apply {
                groupMessageResult?.failures?.let(::addAll)
            }

            if (imageGroupIdsToClear.isNotEmpty() || shouldClearOtherImages) {
                val imageResult = withContext(Dispatchers.IO) {
                    clearSelectedImages(
                        context = context,
                        diskCache = context.imageLoader.diskCache,
                        memoryCache = context.imageLoader.memoryCache,
                        allItems = currentItems,
                        selectedImageGroupIds = imageGroupIdsToClear,
                        clearOtherImages = shouldClearOtherImages,
                        clearAllImages = shouldClearAllImages,
                        resolveGroupName = { id -> entryNamesById[id] ?: emptyGroup },
                        otherImagesLabel = "$otherLabel • $cachedImagesLabel",
                        cachedImagesLabel = cachedImagesLabel,
                    )
                }
                totalCleared += imageResult.clearedCount
                totalFreed += imageResult.freedBytes
                allFailures += imageResult.failures
            }

            selectedMessageGroupIds = emptySet()
            selectedImageGroupIds = emptySet()
            clearOtherImagesSelected = false
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
                    Box {
                        TextButton(
                            onClick = {
                                showSelectAllMenu = true
                            },
                            enabled = !isClearing && !isLoading,
                        ) {
                            Text(text = stringResource(R.string.select_all))
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                            )
                        }

                        Dropdown(
                            expanded = showSelectAllMenu,
                            onDismissRequest = {
                                showSelectAllMenu = false
                            },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.images)) },
                                enabled = hasAnyCachedImages,
                                onClick = {
                                    showSelectAllMenu = false
                                    selectedMessageGroupIds = emptySet()
                                    selectedImageGroupIds = allImageGroupIds
                                    clearOtherImagesSelected = hasOtherImages
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.messages)) },
                                enabled = hasAnyCachedMessages,
                                onClick = {
                                    showSelectAllMenu = false
                                    selectedMessageGroupIds = allMessageGroupIds
                                    selectedImageGroupIds = emptySet()
                                    clearOtherImagesSelected = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.both)) },
                                enabled = hasAnyCachedImages || hasAnyCachedMessages,
                                onClick = {
                                    showSelectAllMenu = false
                                    selectedMessageGroupIds = allMessageGroupIds
                                    selectedImageGroupIds = allImageGroupIds
                                    clearOtherImagesSelected = hasOtherImages
                                },
                            )
                            if (selectedCount > 0) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.deselect_all)) },
                                    onClick = {
                                        showSelectAllMenu = false
                                        selectedMessageGroupIds = emptySet()
                                        selectedImageGroupIds = emptySet()
                                        clearOtherImagesSelected = false
                                    },
                                )
                            }
                        }
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
                        itemsIndexed(
                            items = groupItems,
                            key = { _, item -> item.groupId },
                        ) { index, item ->
                            val isMessagesChecked = item.groupId in selectedMessageGroupIds
                            val isImagesChecked = item.groupId in selectedImageGroupIds

                            GroupCacheSection(
                                title = resolveGroupName(item.entry),
                                messageCount = item.messageCount,
                                messageSizeText = if (item.messageSizeBytes > 0L) {
                                    Formatter.formatShortFileSize(context, item.messageSizeBytes)
                                } else {
                                    null
                                },
                                messagesChecked = isMessagesChecked,
                                messagesEnabled = !isClearing && item.messageSizeBytes > 0L,
                                onToggleMessages = {
                                    selectedMessageGroupIds = if (isMessagesChecked) {
                                        selectedMessageGroupIds - item.groupId
                                    } else {
                                        selectedMessageGroupIds + item.groupId
                                    }
                                },
                                imageCount = item.imageCount,
                                imageSizeText = if (item.imageSizeBytes > 0L) {
                                    Formatter.formatShortFileSize(context, item.imageSizeBytes)
                                } else {
                                    null
                                },
                                imagesChecked = isImagesChecked,
                                imagesEnabled = !isClearing && item.cachedImages.isNotEmpty(),
                                onToggleImages = {
                                    selectedImageGroupIds = if (isImagesChecked) {
                                        selectedImageGroupIds - item.groupId
                                    } else {
                                        selectedImageGroupIds + item.groupId
                                    }
                                },
                            )

                            if (index < groupItems.lastIndex || hasOtherImages) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 2.pad, vertical = .5f.pad)
                                )
                            }
                        }

                        if (hasOtherImages) {
                            item(key = OTHER_IMAGES_ITEM_ID) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 1.pad, bottom = .5f.pad)
                                ) {
                                    Text(
                                        text = otherLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 2.pad, vertical = .5f.pad)
                                    )
                                    CacheOptionCheckboxRow(
                                        label = if (otherImageCount > 0) {
                                            pluralStringResource(
                                                R.plurals.cached_images_count,
                                                otherImageCount,
                                                otherImageCount,
                                            )
                                        } else {
                                            cachedImagesLabel
                                        },
                                        sizeText = if (otherImageBytes > 0L) {
                                            Formatter.formatShortFileSize(context, otherImageBytes)
                                        } else {
                                            null
                                        },
                                        checked = clearOtherImagesSelected,
                                        enabled = !isClearing,
                                        onToggle = {
                                            clearOtherImagesSelected = !clearOtherImagesSelected
                                        },
                                    )
                                }
                            }
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
private fun GroupCacheSection(
    title: String,
    messageCount: Int,
    messageSizeText: String?,
    messagesChecked: Boolean,
    messagesEnabled: Boolean,
    onToggleMessages: () -> Unit,
    imageCount: Int,
    imageSizeText: String?,
    imagesChecked: Boolean,
    imagesEnabled: Boolean,
    onToggleImages: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.pad, bottom = .5f.pad)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.pad, vertical = .5f.pad)
        )
        CacheOptionCheckboxRow(
            label = pluralStringResource(
                R.plurals.cached_messages_count,
                messageCount,
                messageCount,
            ),
            sizeText = messageSizeText,
            checked = messagesChecked,
            enabled = messagesEnabled,
            onToggle = onToggleMessages,
        )
        CacheOptionCheckboxRow(
            label = pluralStringResource(
                R.plurals.cached_images_count,
                imageCount,
                imageCount,
            ),
            sizeText = imageSizeText,
            checked = imagesChecked,
            enabled = imagesEnabled,
            onToggle = onToggleImages,
        )
    }
}

@Composable
private fun CacheOptionCheckboxRow(
    label: String,
    sizeText: String?,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val sizeColor = if (enabled) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onToggle()
            }
            .padding(start = 1.5f.pad, end = 2.pad)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                onToggle()
            },
            enabled = enabled,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 1.pad)
        )
        if (sizeText != null) {
            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelLarge,
                color = sizeColor,
            )
        }
    }
}

private fun inspectImageCache(
    diskCache: DiskCache?,
    entries: List<CachedGroupEntry>,
    groupsLookup: Map<String, GroupExtended>,
): LoadedImageCacheState {
    val totalImageCacheBytes = runCatching {
        diskCache?.size ?: 0L
    }.getOrDefault(0L)

    val snapshotByUrl = HashMap<String, CachedImageRef?>()

    val groupItems = entries.map { entry ->
        val candidateUrls = LinkedHashSet<String>().apply {
            addAll(entry.imageUrls)
            if (entry.group == null) {
                groupsLookup[entry.groupId]?.let { fallbackGroup ->
                    addAll(collectGroupImageUrls(group = fallbackGroup, messages = emptyList()))
                }
            }
        }

        val cachedImages = candidateUrls.mapNotNull { url ->
            snapshotByUrl.getOrPut(url) {
                if (diskCache == null) {
                    null
                } else {
                    runCatching {
                        val snapshot = diskCache.openSnapshot(url) ?: return@runCatching null
                        try {
                            val dataSize = diskCache.fileSystem.metadataOrNull(snapshot.data)?.size ?: 0L
                            val metaSize = diskCache.fileSystem.metadataOrNull(snapshot.metadata)?.size ?: 0L
                            CachedImageRef(
                                url = url,
                                dataFileName = snapshot.data.name,
                                sizeBytes = dataSize + metaSize,
                            )
                        } finally {
                            snapshot.close()
                        }
                    }.getOrNull()
                }
            }
        }

        GroupCacheItem(
            entry = entry,
            cachedImages = cachedImages,
        )
    }.sortedByDescending { it.totalSizeBytes }

    val attributedByDataFile = linkedMapOf<String, Long>()
    groupItems.forEach { item ->
        item.cachedImages.forEach { ref ->
            attributedByDataFile.putIfAbsent(ref.dataFileName, ref.sizeBytes)
        }
    }
    val attributedBytes = attributedByDataFile.values.sum()

    val otherImageCount = diskCache?.let { cache ->
        runCatching {
            cache.directory.toFile().listFiles { file ->
                file.isFile && file.name.endsWith(".1") && file.name !in attributedByDataFile
            }?.size ?: 0
        }.getOrDefault(0)
    } ?: 0

    val otherImageBytes = (totalImageCacheBytes - attributedBytes).coerceAtLeast(0L)

    return LoadedImageCacheState(
        groupItems = groupItems,
        totalImageCacheBytes = totalImageCacheBytes,
        otherImageCount = otherImageCount,
        otherImageBytes = otherImageBytes,
    )
}

private data class ClearImageCacheResult(
    val clearedCount: Int,
    val freedBytes: Long,
    val failures: List<ClearCacheFailure>,
)

private fun clearSelectedImages(
    context: Context,
    diskCache: DiskCache?,
    memoryCache: MemoryCache?,
    allItems: List<GroupCacheItem>,
    selectedImageGroupIds: Set<String>,
    clearOtherImages: Boolean,
    clearAllImages: Boolean,
    resolveGroupName: (String) -> String,
    otherImagesLabel: String,
    cachedImagesLabel: String,
): ClearImageCacheResult {
    if (diskCache == null) {
        return ClearImageCacheResult(
            clearedCount = 0,
            freedBytes = 0L,
            failures = emptyList(),
        )
    }

    val sizeBefore = runCatching { diskCache.size }.getOrDefault(0L)
    val failures = mutableListOf<ClearCacheFailure>()

    if (clearAllImages) {
        return runCatching {
            diskCache.clear()
            memoryCache?.clear()
        }.fold(
            onSuccess = {
                val clearedItems = selectedImageGroupIds.size + if (clearOtherImages) 1 else 0
                ClearImageCacheResult(
                    clearedCount = clearedItems,
                    freedBytes = sizeBefore,
                    failures = emptyList(),
                )
            },
            onFailure = { error ->
                ClearImageCacheResult(
                    clearedCount = 0,
                    freedBytes = 0L,
                    failures = listOf(
                        ClearCacheFailure(
                            id = OTHER_IMAGES_ITEM_ID,
                            name = cachedImagesLabel,
                            reason = error.localizedMessage ?: error.javaClass.simpleName,
                        )
                    ),
                )
            },
        )
    }

    if (!clearOtherImages) {
        var clearedCount = 0
        val removedUrls = HashSet<String>()

        allItems
            .filter { it.groupId in selectedImageGroupIds }
            .forEach { item ->
                runCatching {
                    item.cachedImages.forEach { ref ->
                        if (removedUrls.add(ref.url)) {
                            diskCache.remove(ref.url)
                        }
                    }
                }.onSuccess {
                    clearedCount++
                }.onFailure { error ->
                    failures += ClearCacheFailure(
                        id = "${item.groupId}:images",
                        name = "${resolveGroupName(item.groupId)} • $cachedImagesLabel",
                        reason = error.localizedMessage ?: error.javaClass.simpleName,
                    )
                }
            }

        if (removedUrls.isNotEmpty() && memoryCache != null) {
            runCatching {
                memoryCache.keys
                    .filter { it.key in removedUrls }
                    .forEach { memoryCache.remove(it) }
            }
        }

        val sizeAfter = runCatching { diskCache.size }.getOrDefault(0L)
        return ClearImageCacheResult(
            clearedCount = clearedCount,
            freedBytes = (sizeBefore - sizeAfter).coerceAtLeast(0L),
            failures = failures,
        )
    }

    // Clearing "Other" cached images while preserving images for unselected groups.
    val selectedUrls = allItems
        .filter { it.groupId in selectedImageGroupIds }
        .flatMap { item -> item.cachedImages.map { it.url } }
        .toSet()

    val urlsToKeep = allItems
        .filter { it.groupId !in selectedImageGroupIds }
        .flatMap { item -> item.cachedImages.map { it.url } }
        .filterNot { it in selectedUrls }
        .distinct()

    val tempDir = File(context.cacheDir, "cache_preserve_${System.nanoTime()}")
    return try {
        val preserved = mutableListOf<Triple<String, File, File>>()
        if (urlsToKeep.isNotEmpty()) {
            tempDir.mkdirs()
            urlsToKeep.forEachIndexed { index, url ->
                val snapshot = diskCache.openSnapshot(url)
                if (snapshot != null) {
                    try {
                        val metaTemp = File(tempDir, "$index.0")
                        val dataTemp = File(tempDir, "$index.1")
                        diskCache.fileSystem.copy(snapshot.metadata, metaTemp.toOkioPath())
                        diskCache.fileSystem.copy(snapshot.data, dataTemp.toOkioPath())
                        preserved += Triple(url, metaTemp, dataTemp)
                    } finally {
                        snapshot.close()
                    }
                }
            }
        }

        diskCache.clear()
        memoryCache?.clear()

        preserved.forEach { (url, metaTemp, dataTemp) ->
            val editor = diskCache.openEditor(url)
            if (editor != null) {
                try {
                    diskCache.fileSystem.copy(metaTemp.toOkioPath(), editor.metadata)
                    diskCache.fileSystem.copy(dataTemp.toOkioPath(), editor.data)
                    editor.commit()
                } catch (t: Throwable) {
                    runCatching { editor.abort() }
                    throw t
                }
            }
        }

        val sizeAfter = runCatching { diskCache.size }.getOrDefault(0L)
        ClearImageCacheResult(
            clearedCount = selectedImageGroupIds.size + 1,
            freedBytes = (sizeBefore - sizeAfter).coerceAtLeast(0L),
            failures = emptyList(),
        )
    } catch (error: Throwable) {
        ClearImageCacheResult(
            clearedCount = 0,
            freedBytes = 0L,
            failures = listOf(
                ClearCacheFailure(
                    id = OTHER_IMAGES_ITEM_ID,
                    name = otherImagesLabel,
                    reason = error.localizedMessage ?: error.javaClass.simpleName,
                )
            ),
        )
    } finally {
        runCatching { tempDir.deleteRecursively() }
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
