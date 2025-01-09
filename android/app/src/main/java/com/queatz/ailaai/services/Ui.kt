package com.queatz.ailaai.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.queatz.ailaai.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val ui by lazy {
    Ui()
}

private val hiddenDescriptionsKey = stringSetPreferencesKey("hiddenDescriptions")

class Ui {
    private lateinit var context: Context
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _showOfflineNote = MutableStateFlow(false)
    val showOfflineNote = _showOfflineNote.asStateFlow()

    private val hiddenDescriptions = mutableSetOf<String>()

    fun init(context: Context) {
        this.context = context
        scope.launch {
            hiddenDescriptions.addAll(
                context.dataStore.data.first()[hiddenDescriptionsKey] ?: emptySet()
            )
        }
    }

    fun showOfflineNote(show: Boolean) {
        _showOfflineNote.value = show
    }

    fun setShowDescription(groupId: String, showDescription: Boolean) {
        if (showDescription) {
            hiddenDescriptions.remove(groupId)
        } else {
            hiddenDescriptions.add(groupId)
        }
        scope.launch {
            context.dataStore.edit {
                it[hiddenDescriptionsKey] = hiddenDescriptions
            }
        }
    }

    fun getShowDescription(groupId: String) =
        !hiddenDescriptions.contains(groupId)
}
