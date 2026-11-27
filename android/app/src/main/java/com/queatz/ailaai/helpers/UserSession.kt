package com.queatz.ailaai.helpers

import android.content.Context
import coil3.imageLoader
import com.queatz.ailaai.cache.cache
import com.queatz.ailaai.cache.groupMessageCache
import com.queatz.ailaai.data.api
import com.queatz.ailaai.db.db
import com.queatz.ailaai.services.*
import com.queatz.ailaai.ui.components.clearReactionsCache
import com.queatz.ailaai.ui.control.clearMapCardsCache
import com.queatz.ailaai.ui.dialogs.clearCategoriesCache
import com.queatz.ailaai.ui.screens.clearFriendsScreenCache
import com.queatz.ailaai.ui.screens.clearGroupsScreenCache
import com.queatz.ailaai.ui.screens.clearScheduleScreenCache
import com.queatz.ailaai.ui.story.clearSheetScreenCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserSession {
    suspend fun clear(context: Context) {
        clearFriendsScreenCache()
        clearGroupsScreenCache()
        clearScheduleScreenCache()
        clearSheetScreenCache()
        clearMapCardsCache()
        clearReactionsCache()
        clearCategoriesCache()

        messages.clear()
        signals.clear()
        chips.clear()
        saves.clear()
        joins.clear()
        calls.clear()
        mePresence.clear()
        stickers.clear()
        push.clear()

        cache.clear()
        db.clear()
        groupMessageCache.clear()

        withContext(Dispatchers.Main) {
            runCatching {
                context.imageLoader.memoryCache?.clear()
            }
        }
    }

    suspend fun signOut(context: Context) {
        api.signOut()
        clear(context)
    }
}
