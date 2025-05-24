package com.queatz.api

import io.ktor.server.routing.Route

fun Route.gameRoutes() {
    gameSceneRoutes()
    gameTileRoutes()
    gameObjectRoutes()
    gameMusicRoutes()
    gameDiscussionRoutes()
}
