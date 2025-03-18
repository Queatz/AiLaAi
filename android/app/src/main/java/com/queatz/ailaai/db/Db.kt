package com.queatz.ailaai.db

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor

val db by lazy {
    Db()
}

class Db() {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        runCatching {
            store = MyObjectBox.builder()
                .androidContext(context)
                .build()
        }.onFailure {
            BoxStore.deleteAllFiles(context, null)
            init(context)
        }
    }

    inline fun <reified T : Any> box() = store.boxFor<T>()

    fun clear() {
        if (!store.isClosed) {
            store.close()
        }

        store.deleteAllFiles()
    }
}
