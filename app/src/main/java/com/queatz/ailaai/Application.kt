package com.queatz.ailaai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import at.bluesource.choicesdk.core.ChoiceSdk

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        ChoiceSdk.init(this)
        api.init(this)
    }
}
