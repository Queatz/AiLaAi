package com.queatz.ailaai

import at.bluesource.choicesdk.core.ChoiceSdk

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        ChoiceSdk.init(this)
    }
}
