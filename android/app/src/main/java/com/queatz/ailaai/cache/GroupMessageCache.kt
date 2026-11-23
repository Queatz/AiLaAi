package com.queatz.ailaai.cache

import android.content.Context
import android.util.LruCache
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.getAllAttachments
import com.queatz.ailaai.data.json
import com.queatz.ailaai.dataStore
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import com.queatz.db.PhotosAttachment
import com.queatz.db.StickerAttachment
import com.queatz.db.UrlAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val MAX_CACHED_MESSAGES_PER_GROUP = 100
const val IMAGE_CACHE_DIR_NAME = "image_cache"
private const val MAX_IN_MEMORY_GROUPS = 20
private const val MESSAGE_CACHE_DIR_NAME = "group_messages"

private val messageCacheEnabledKey = booleanPreferencesKey("cache.messages.enabled")
private val cacheModeKey = stringPreferencesKey("cache.mode")
private val cacheSizeLimitKey = stringPreferencesKey("cache.size.limit")

enum class CacheMode(val key: String) {
    None("none"),
    Recent100("recent_100"),
    All("all"),
    ;

    companion object {
        val Default = Recent100

        fun fromKey(key: String?): CacheMode? = entries.firstOrNull { it.key == key }
    }
}

enum class CacheSizeLimit(
    val key: String,
    val totalBytes: Long?,
) {
    Mb256("256mb", 256L * 1024 * 1024),
    Mb512("512mb", 512L * 1024 * 1024),
    Gb1("1gb", 1L * 1024 * 1024 * 1024),
    Gb2("2gb", 2L * 1024 * 1024 * 1024),
    Gb4("4gb", 4L * 1024 * 1024 * 1024),
    Gb8("8gb", 8L * 1024 * 1024 * 1024),
    Gb16("16gb", 16L * 1024 * 1024 * 1024),
    Gb32("32gb", 32L * 1024 * 1024 * 1024),
    Gb64("64gb", 64L * 1024 * 1024 * 1024),
    Unlimited("unlimited", null),
    ;

    /** 25% of total budget for chat message cache, paired with 75% for image cache. */
    val messageBudgetBytes: Long?
        get() = totalBytes?.div(4)

    /** 75% of total budget for image disk cache, paired with 25% for message cache. */
    val imageBudgetBytes: Long?
        get() = totalBytes?.let { it - (it / 4) }

    companion object {
        val Default = Gb1

        fun fromKey(key: String?): CacheSizeLimit? = entries.firstOrNull { it.key == key }
    }
}

private val imageLoaderMutex = Mutex()

fun buildAppImageLoader(
    context: Context,
    mode: CacheMode,
    sizeLimit: CacheSizeLimit,
): ImageLoader {
    val appContext = context.applicationContext
    val builder = ImageLoader.Builder(appContext)
    if (mode == CacheMode.None) {
        builder
            .diskCachePolicy(CachePolicy.DISABLED)
            .diskCache(null)
    } else {
        val maxImageBytes = sizeLimit.imageBudgetBytes ?: Long.MAX_VALUE
        builder.diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve(IMAGE_CACHE_DIR_NAME))
                .maxSizeBytes(maxImageBytes)
                .build()
        }
    }
    return builder.build()
}

@OptIn(DelicateCoilApi::class)
suspend fun reconfigureAppImageLoader(
    context: Context,
    mode: CacheMode,
    sizeLimit: CacheSizeLimit,
    trimToLimit: Boolean,
) {
    val appContext = context.applicationContext
    withContext(Dispatchers.IO) {
        imageLoaderMutex.withLock {
            val imageCacheDir = appContext.cacheDir.resolve(IMAGE_CACHE_DIR_NAME)
            val oldLoader = runCatching { SingletonImageLoader.get(appContext) }.getOrNull()
            val oldDiskCache = oldLoader?.diskCache

            if (mode == CacheMode.None) {
                runCatching { oldLoader?.shutdown() }
                runCatching { oldDiskCache?.clear() }
                runCatching { oldDiskCache?.shutdown() }
                runCatching { imageCacheDir.deleteRecursively() }
            } else {
                runCatching { oldLoader?.shutdown() }
                runCatching { oldDiskCache?.shutdown() }

                val maxImageBytes = sizeLimit.imageBudgetBytes
                if (trimToLimit && maxImageBytes != null && imageCacheDir.exists()) {
                    runCatching {
                        val trimCache = DiskCache.Builder()
                            .directory(imageCacheDir)
                            .maxSizeBytes(maxImageBytes)
                            .build()
                        try {
                            // Reading size initializes the journal; shutdown() trims LRU entries to maxSize.
                            trimCache.size
                        } finally {
                            trimCache.shutdown()
                        }
                    }
                }
            }

            val newLoader = buildAppImageLoader(
                context = appContext,
                mode = mode,
                sizeLimit = sizeLimit,
            )
            SingletonImageLoader.setUnsafe(newLoader)
        }
    }
}

val groupMessageCache by lazy {
    GroupMessageCache()
}

@Serializable
data class CachedGroupData(
    val group: GroupExtended? = null,
    val messages: List<Message> = emptyList(),
    val hasOlderMessages: Boolean = true,
)

data class CachedGroupEntry(
    val groupId: String,
    val group: GroupExtended?,
    val messageCount: Int,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val imageUrls: List<String> = emptyList(),
)

fun collectGroupImageUrls(
    group: GroupExtended?,
    messages: List<Message>,
): List<String> {
    val urls = LinkedHashSet<String>()

    group?.group?.photo?.takeIf { it.isNotBlank() }?.let { urls += api.url(it) }
    group?.group?.background?.takeIf { it.isNotBlank() }?.let { urls += api.url(it) }
    group?.members?.forEach { member ->
        member.person?.photo?.takeIf { it.isNotBlank() }?.let { urls += api.url(it) }
    }
    group?.bots?.forEach { bot ->
        bot.photo?.takeIf { it.isNotBlank() }?.let { urls += api.url(it) }
    }

    messages.forEach { message ->
        runCatching { message.getAllAttachments() }.getOrNull()?.forEach { attachment ->
            when (attachment) {
                is PhotosAttachment -> {
                    attachment.photos?.forEach { photo ->
                        if (photo.isNotBlank()) {
                            urls += api.url(photo)
                        }
                    }
                }

                is StickerAttachment -> {
                    attachment.photo?.takeIf { it.isNotBlank() }?.let { urls += api.url(it) }
                }

                is UrlAttachment -> {
                    attachment.image?.takeIf { it.isNotBlank() }?.let { urls += it }
                }

                else -> Unit
            }
        }
    }

    return urls.toList()
}

data class ClearCacheFailure(
    val id: String,
    val name: String,
    val reason: String,
)

data class ClearCacheResult(
    val clearedCount: Int,
    val freedBytes: Long,
    val failures: List<ClearCacheFailure>,
)

/**
 * Merges a freshly fetched page of recent messages (sorted newest-first) with previously cached messages.
 *
 * - If [fresh] is empty, all messages are cleared.
 * - If [fresh] overlaps or is contiguous with [cached] (`fresh.last().createdAt <= cached.first().createdAt`),
 *   [fresh] is authoritative for `[fresh.last().createdAt .. now]` (reflecting edits, reaction updates,
 *   and deletions in that range), while strictly older cached messages are retained up to [maxCount].
 * - If there is a gap (`fresh.last().createdAt > cached.first().createdAt`), stale older cached messages
 *   are discarded to avoid displaying a silent gap of missing messages.
 */
fun mergeCachedMessages(
    cached: List<Message>,
    fresh: List<Message>,
    maxCount: Int = MAX_CACHED_MESSAGES_PER_GROUP,
): List<Message> {
    if (fresh.isEmpty()) {
        return emptyList()
    }

    val normalizedFresh = fresh.distinctBy { it.id }

    if (cached.isEmpty()) {
        return normalizedFresh.take(maxCount)
    }

    val oldestFresh = normalizedFresh.lastOrNull()?.createdAt
    val newestCached = cached.firstOrNull()?.createdAt

    if (oldestFresh == null || newestCached == null || oldestFresh > newestCached) {
        return normalizedFresh.take(maxCount)
    }

    val olderCached = cached.filter { message ->
        val createdAt = message.createdAt
        createdAt != null && createdAt < oldestFresh
    }

    return (normalizedFresh + olderCached)
        .distinctBy { it.id }
        .take(maxCount)
}

class GroupMessageCache {
    private lateinit var context: Context
    private lateinit var cacheDir: File

    private val memoryCache = LruCache<String, CachedGroupData>(MAX_IN_MEMORY_GROUPS)
    private val diskMutex = Mutex()

    private val _mode = MutableStateFlow(CacheMode.Default)
    val mode = _mode.asStateFlow()

    private val _sizeLimit = MutableStateFlow(CacheSizeLimit.Default)
    val sizeLimit = _sizeLimit.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (isInitialized()) {
            return
        }

        this.context = context.applicationContext
        this.cacheDir = File(this.context.cacheDir, MESSAGE_CACHE_DIR_NAME)

        val preferences = runCatching {
            runBlocking {
                this@GroupMessageCache.context.dataStore.data.first()
            }
        }.getOrNull()

        val initialMode = CacheMode.fromKey(preferences?.get(cacheModeKey))
            ?: if (preferences?.get(messageCacheEnabledKey) == false) {
                CacheMode.None
            } else {
                CacheMode.Default
            }

        val initialSizeLimit = CacheSizeLimit.fromKey(preferences?.get(cacheSizeLimitKey))
            ?: CacheSizeLimit.Default

        _mode.value = initialMode
        _sizeLimit.value = initialSizeLimit
    }

    fun isInitialized(): Boolean = ::cacheDir.isInitialized

    fun getInMemory(groupId: String): CachedGroupData? {
        if (_mode.value == CacheMode.None) {
            return null
        }
        return memoryCache.get(groupId)
    }

    suspend fun get(groupId: String): CachedGroupData? {
        if (_mode.value == CacheMode.None || !isInitialized()) {
            return null
        }

        memoryCache.get(groupId)?.let { return it }

        return withContext(Dispatchers.IO) {
            val file = fileFor(groupId)
            if (!file.exists()) {
                return@withContext null
            }

            runCatching {
                val data = json.decodeFromString<CachedGroupData>(file.readText())
                file.setLastModified(System.currentTimeMillis())
                memoryCache.put(groupId, data)
                data
            }.onFailure {
                runCatching { file.delete() }
            }.getOrNull()
        }
    }

    suspend fun putMessages(
        groupId: String,
        group: GroupExtended? = null,
        messages: List<Message>,
        hasOlderMessages: Boolean = true,
    ) {
        if (_mode.value == CacheMode.None || !isInitialized()) {
            return
        }

        val distinct = messages.distinctBy { it.id }

        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val trimmed = when (_mode.value) {
                    CacheMode.None -> return@withLock
                    CacheMode.Recent100 -> distinct.take(MAX_CACHED_MESSAGES_PER_GROUP)
                    CacheMode.All -> distinct
                }
                val effectiveHasOlderMessages = hasOlderMessages || trimmed.size < distinct.size

                val existing = memoryCache.get(groupId) ?: readFromDiskUnchecked(groupId)
                val updated = CachedGroupData(
                    group = group ?: existing?.group,
                    messages = trimmed,
                    hasOlderMessages = effectiveHasOlderMessages,
                )

                memoryCache.put(groupId, updated)
                writeToDiskUnchecked(
                    groupId = groupId,
                    data = updated,
                )
                pruneIfOverBudgetUnchecked()
            }
        }
    }

    suspend fun putGroup(
        groupId: String,
        group: GroupExtended,
    ) {
        if (_mode.value == CacheMode.None || !isInitialized()) {
            return
        }

        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                if (_mode.value == CacheMode.None) {
                    return@withLock
                }

                val existing = memoryCache.get(groupId) ?: readFromDiskUnchecked(groupId)
                val updated = (existing ?: CachedGroupData()).copy(group = group)
                memoryCache.put(groupId, updated)
                writeToDiskUnchecked(
                    groupId = groupId,
                    data = updated,
                )
                pruneIfOverBudgetUnchecked()
            }
        }
    }

    suspend fun remove(groupId: String): Result<Long> {
        memoryCache.remove(groupId)

        if (!isInitialized()) {
            return Result.success(0L)
        }

        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                removeFileUnchecked(groupId)
            }
        }
    }

    suspend fun removeMultiple(
        groupIds: Collection<String>,
        resolveName: (String) -> String,
    ): ClearCacheResult {
        if (!isInitialized()) {
            return ClearCacheResult(
                clearedCount = 0,
                freedBytes = 0L,
                failures = emptyList(),
            )
        }

        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                var clearedCount = 0
                var freedBytes = 0L
                val failures = mutableListOf<ClearCacheFailure>()

                groupIds.forEach { groupId ->
                    memoryCache.remove(groupId)
                    removeFileUnchecked(groupId)
                        .onSuccess { bytes ->
                            clearedCount++
                            freedBytes += bytes
                        }
                        .onFailure { error ->
                            failures += ClearCacheFailure(
                                id = groupId,
                                name = resolveName(groupId),
                                reason = error.localizedMessage ?: error.javaClass.simpleName,
                            )
                        }
                }

                ClearCacheResult(
                    clearedCount = clearedCount,
                    freedBytes = freedBytes,
                    failures = failures,
                )
            }
        }
    }

    suspend fun listEntries(): Result<List<CachedGroupEntry>> {
        if (!isInitialized()) {
            return Result.success(emptyList())
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                diskMutex.withLock {
                    if (!cacheDir.exists()) {
                        return@withLock emptyList()
                    }

                    val files = cacheDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".json")
                    } ?: return@withLock emptyList()

                    files.mapNotNull { file ->
                        val groupId = decodeGroupId(file.name.removeSuffix(".json")) ?: return@mapNotNull null
                        val parsed = runCatching {
                            json.decodeFromString<CachedGroupData>(file.readText())
                        }.getOrNull()

                        CachedGroupEntry(
                            groupId = groupId,
                            group = parsed?.group,
                            messageCount = parsed?.messages?.size ?: 0,
                            sizeBytes = file.length(),
                            lastModifiedMillis = file.lastModified(),
                            imageUrls = collectGroupImageUrls(
                                group = parsed?.group,
                                messages = parsed?.messages.orEmpty(),
                            ),
                        )
                    }.sortedByDescending { it.sizeBytes }
                }
            }
        }
    }

    suspend fun sizeBytes(): Long {
        if (!isInitialized()) {
            return 0L
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                if (!cacheDir.exists()) {
                    return@runCatching 0L
                }
                cacheDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".json")
                }?.sumOf { it.length() } ?: 0L
            }.getOrDefault(0L)
        }
    }

    suspend fun clear(): Result<Long> {
        memoryCache.evictAll()

        if (!isInitialized()) {
            return Result.success(0L)
        }

        return withContext(Dispatchers.IO) {
            diskMutex.withLock {
                runCatching {
                    if (!cacheDir.exists()) {
                        return@runCatching 0L
                    }

                    var freed = 0L
                    cacheDir.listFiles()?.forEach { file ->
                        val size = if (file.isFile) file.length() else 0L
                        if (file.delete()) {
                            freed += size
                        }
                    }
                    freed
                }
            }
        }
    }

    suspend fun estimateBytesDeletedOnModeChange(
        targetMode: CacheMode,
        currentImageCacheBytes: Long,
    ): Long {
        if (targetMode.ordinal >= _mode.value.ordinal || !isInitialized()) {
            return 0L
        }

        return when (targetMode) {
            CacheMode.None -> sizeBytes() + currentImageCacheBytes.coerceAtLeast(0L)

            CacheMode.Recent100 -> withContext(Dispatchers.IO) {
                diskMutex.withLock {
                    if (!cacheDir.exists()) {
                        return@withLock 0L
                    }

                    val files = cacheDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".json")
                    } ?: return@withLock 0L

                    var freed = 0L
                    files.forEach { file ->
                        val existing = runCatching {
                            json.decodeFromString<CachedGroupData>(file.readText())
                        }.getOrNull() ?: return@forEach

                        if (existing.messages.size > MAX_CACHED_MESSAGES_PER_GROUP) {
                            val trimmed = existing.copy(
                                messages = existing.messages.take(MAX_CACHED_MESSAGES_PER_GROUP),
                                hasOlderMessages = true,
                            )
                            val newSize = runCatching {
                                json.encodeToString(trimmed).toByteArray(StandardCharsets.UTF_8).size.toLong()
                            }.getOrDefault(file.length())
                            freed += (file.length() - newSize).coerceAtLeast(0L)
                        }
                    }
                    freed
                }
            }

            CacheMode.All -> 0L
        }
    }

    suspend fun estimateBytesDeletedOnSizeLimitChange(
        targetLimit: CacheSizeLimit,
        currentImageCacheBytes: Long,
    ): Long {
        if (targetLimit.ordinal >= _sizeLimit.value.ordinal || !isInitialized() || _mode.value == CacheMode.None) {
            return 0L
        }

        val maxMessageBytes = targetLimit.messageBudgetBytes
        val messageBytesToDelete = if (maxMessageBytes == null) {
            0L
        } else {
            withContext(Dispatchers.IO) {
                diskMutex.withLock {
                    if (!cacheDir.exists()) {
                        return@withLock 0L
                    }

                    val files = cacheDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".json")
                    } ?: return@withLock 0L

                    var totalBytes = files.sumOf { it.length() }
                    if (totalBytes <= maxMessageBytes) {
                        return@withLock 0L
                    }

                    var freed = 0L
                    val oldestFirst = files.sortedBy { it.lastModified() }
                    for (file in oldestFirst) {
                        if (totalBytes <= maxMessageBytes) {
                            break
                        }
                        val fileSize = file.length()
                        totalBytes -= fileSize
                        freed += fileSize
                    }
                    freed
                }
            }
        }

        val maxImageBytes = targetLimit.imageBudgetBytes
        val imageBytesToDelete = if (maxImageBytes == null) {
            0L
        } else {
            (currentImageCacheBytes.coerceAtLeast(0L) - maxImageBytes).coerceAtLeast(0L)
        }

        return messageBytesToDelete + imageBytesToDelete
    }

    suspend fun setMode(mode: CacheMode): Result<Unit> {
        val previousMode = _mode.value
        _mode.value = mode

        if (!isInitialized()) {
            return Result.success(Unit)
        }

        val persistResult = runCatching {
            context.dataStore.edit { preferences ->
                preferences[cacheModeKey] = mode.key
                preferences[messageCacheEnabledKey] = mode != CacheMode.None
            }
            Unit
        }

        if (persistResult.isFailure) {
            _mode.value = previousMode
            return persistResult
        }

        if (mode == CacheMode.None) {
            clear()
            reconfigureAppImageLoader(
                context = context,
                mode = CacheMode.None,
                sizeLimit = _sizeLimit.value,
                trimToLimit = false,
            )
        } else {
            if (previousMode == CacheMode.All && mode == CacheMode.Recent100) {
                trimAllGroupsToRecentLimit()
            }
            if (previousMode == CacheMode.None) {
                reconfigureAppImageLoader(
                    context = context,
                    mode = mode,
                    sizeLimit = _sizeLimit.value,
                    trimToLimit = false,
                )
            }
        }

        return persistResult
    }

    suspend fun setSizeLimit(limit: CacheSizeLimit): Result<Unit> {
        val previousLimit = _sizeLimit.value
        _sizeLimit.value = limit

        if (!isInitialized()) {
            return Result.success(Unit)
        }

        val persistResult = runCatching {
            context.dataStore.edit { preferences ->
                preferences[cacheSizeLimitKey] = limit.key
            }
            Unit
        }

        if (persistResult.isFailure) {
            _sizeLimit.value = previousLimit
            return persistResult
        }

        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                pruneIfOverBudgetUnchecked()
            }
        }

        val currentMode = _mode.value
        if (currentMode != CacheMode.None) {
            val newImageBudget = limit.imageBudgetBytes
            val oldImageBudget = previousLimit.imageBudgetBytes
            val shouldTrimImages = newImageBudget != null &&
                (oldImageBudget == null || newImageBudget < oldImageBudget)
            reconfigureAppImageLoader(
                context = context,
                mode = currentMode,
                sizeLimit = limit,
                trimToLimit = shouldTrimImages,
            )
        }

        return persistResult
    }

    private suspend fun trimAllGroupsToRecentLimit() {
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                if (cacheDir.exists()) {
                    val files = cacheDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".json")
                    }
                    files?.forEach { file ->
                        val groupId = decodeGroupId(file.name.removeSuffix(".json")) ?: return@forEach
                        val originalLastModified = file.lastModified()
                        val existing = runCatching {
                            json.decodeFromString<CachedGroupData>(file.readText())
                        }.getOrNull() ?: return@forEach

                        if (existing.messages.size > MAX_CACHED_MESSAGES_PER_GROUP) {
                            val trimmed = existing.copy(
                                messages = existing.messages.take(MAX_CACHED_MESSAGES_PER_GROUP),
                                hasOlderMessages = true,
                            )
                            writeToDiskUnchecked(
                                groupId = groupId,
                                data = trimmed,
                                lastModifiedMillis = originalLastModified,
                            )
                            if (memoryCache.get(groupId) != null) {
                                memoryCache.put(groupId, trimmed)
                            }
                        }
                    }
                }

                memoryCache.snapshot().forEach { (groupId, cached) ->
                    if (cached.messages.size > MAX_CACHED_MESSAGES_PER_GROUP) {
                        memoryCache.put(
                            groupId,
                            cached.copy(
                                messages = cached.messages.take(MAX_CACHED_MESSAGES_PER_GROUP),
                                hasOlderMessages = true,
                            ),
                        )
                    }
                }

                pruneIfOverBudgetUnchecked()
            }
        }
    }

    private fun fileFor(groupId: String): File {
        val encoded = URLEncoder.encode(groupId, StandardCharsets.UTF_8.name())
        return File(cacheDir, "$encoded.json")
    }

    private fun decodeGroupId(encoded: String): String? = runCatching {
        URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
    }.getOrNull()

    private fun readFromDiskUnchecked(groupId: String): CachedGroupData? {
        val file = fileFor(groupId)
        if (!file.exists()) {
            return null
        }

        return runCatching {
            json.decodeFromString<CachedGroupData>(file.readText())
        }.getOrNull()
    }

    private fun writeToDiskUnchecked(
        groupId: String,
        data: CachedGroupData,
        lastModifiedMillis: Long = System.currentTimeMillis(),
    ) {
        runCatching {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val target = fileFor(groupId)
            val encoded = json.encodeToString(data)
            val temp = File(cacheDir, "${target.name}.${System.nanoTime()}.tmp")

            temp.writeText(encoded)
            if (!temp.renameTo(target)) {
                target.writeText(encoded)
                temp.delete()
            }
            target.setLastModified(lastModifiedMillis)
        }
    }

    private fun removeFileUnchecked(groupId: String): Result<Long> = runCatching {
        val file = fileFor(groupId)
        if (!file.exists()) {
            return@runCatching 0L
        }

        val size = file.length()
        if (!file.delete() && file.exists()) {
            throw IllegalStateException("Could not delete cached file (${file.name})")
        }
        size
    }

    private fun pruneIfOverBudgetUnchecked() {
        val maxBytes = _sizeLimit.value.messageBudgetBytes ?: return
        runCatching {
            if (!cacheDir.exists()) {
                return
            }

            val files = cacheDir.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            } ?: return

            var totalBytes = files.sumOf { it.length() }
            if (totalBytes <= maxBytes) {
                return
            }

            val oldestFirst = files.sortedBy { it.lastModified() }
            for (file in oldestFirst) {
                if (totalBytes <= maxBytes) {
                    break
                }
                val fileSize = file.length()
                val groupId = decodeGroupId(file.name.removeSuffix(".json"))
                if (file.delete()) {
                    totalBytes -= fileSize
                    if (groupId != null) {
                        memoryCache.remove(groupId)
                    }
                }
            }
        }
    }
}
