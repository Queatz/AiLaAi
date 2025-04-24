package com.queatz.ailaai.slideshow

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.navOptions
import app.ailaai.api.cardsCards
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.toast
import com.queatz.db.Card
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val slideshow by lazy {
    Slideshow()
}

class Slideshow {
    private lateinit var context: Context
    var navController: NavController? = null

    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    private val _fullscreen = MutableStateFlow(false)
    val fullscreen = _fullscreen.asStateFlow()

    private val _card = MutableStateFlow<Card?>(null)
    val card = _card.asStateFlow()

    private val _userIsInactive = MutableStateFlow(false)
    val userIsInactive = _userIsInactive.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var slideshowJob: Job? = null
    private var userInteractionTimeoutJob: Job? = null

    private val userInteractionTapTimeout: Duration = 1.seconds
    private val userInteractionTimeout: Duration = 30.seconds

    fun init(context: Context) {
        this.context = context
    }

    fun setFullscreen(fullscreen: Boolean) {
        this._fullscreen.value = fullscreen
    }

    fun start(
        cardId: String,
        slideDuration: Duration = 2.minutes
    ) {
        cancelUserInteraction()
        context.toast(R.string.slideshow_started)
        slideshowJob?.cancel()
        _active.value = true

        slideshowJob = scope.launch {
            while (true) {
                val cards = mutableListOf<Card>()

                api.cardsCards(cardId) {
                    cards.clear()
                    cards.addAll(it.filter { it.active == true })
                }

                if (cards.isEmpty()) {
                    Log.w("Slideshow", "No cards found")
                    delay(1.minutes)
                    continue
                }

                Log.w("Slideshow", "${cards.size} cards in slideshow")

                while (cards.isNotEmpty()) {
                    val card = cards.removeAt(0)

                    _card.value = card

                    Log.w("Slideshow", "Navigating to card ${card.id!!}")

                    withContext(Dispatchers.Main) {
                        navController!!.appNavigate(
                            AppNav.Page(card.id!!),
                            navOptions {
                                launchSingleTop = true
                                popUpTo(AppNav.Explore.route)
                            }
                        )
                    }

                    delay(slideDuration)
                    userIsInactive.first { it }
                }
            }
        }
    }

    fun stop() {
        _active.value = false
        context.toast(R.string.slideshow_stopped)
        slideshowJob?.cancel()
    }

    fun onUserInteraction() {
        userInteractionTimeoutJob?.cancel()
        userInteractionTimeoutJob = scope.launch {
            delay(userInteractionTapTimeout)
            _userIsInactive.value = false
            delay(userInteractionTimeout)
            _userIsInactive.value = true
        }
    }

    fun cancelUserInteraction() {
        _userIsInactive.value = true
        userInteractionTimeoutJob?.cancel()
    }
}
