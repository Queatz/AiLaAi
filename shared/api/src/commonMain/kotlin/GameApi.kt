package app.ailaai.api

import com.queatz.db.Comment
import com.queatz.db.GameDiscussion
import com.queatz.db.GameDiscussionExtended
import com.queatz.db.GameMusic
import com.queatz.db.GameObject
import com.queatz.db.GameScene
import com.queatz.db.GameTile
import io.ktor.http.HttpStatusCode

// GameScene API functions
suspend fun Api.gameScene(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameScene>,
) = get(
    url = "game-scene/$id",
    errorContext = "load scene",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createGameScene(
    gameScene: GameScene,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameScene>,
) = post(
    url = "game-scene",
    body = gameScene,
    errorContext = "create scene",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateGameScene(
    id: String,
    gameScene: GameScene,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameScene>,
) = post(
    url = "game-scene/$id",
    body = gameScene,
    errorContext = "update scene",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameScene(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-scene/$id/delete",
    errorContext = "delete scene",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameScenes(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameScene>>,
) = get(
    url = "game-scene",
    errorContext = "load scenes",
    onError = onError,
    onSuccess = onSuccess
)

// GameTile API functions
suspend fun Api.gameTile(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameTile>,
) = get(
    url = "game-tile/$id",
    errorContext = "load tile",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameTiles(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameTile>>,
) = get(
    url = "game-tile",
    errorContext = "load tiles",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createGameTile(
    gameTile: GameTile,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameTile>,
) = post(
    url = "game-tile",
    body = gameTile,
    errorContext = "create tile",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateGameTile(
    id: String,
    gameTile: GameTile,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameTile>,
) = post(
    url = "game-tile/$id",
    body = gameTile,
    errorContext = "update tile",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameTile(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-tile/$id/delete",
    errorContext = "delete tile",
    onError = onError,
    onSuccess = onSuccess
)

// GameObject API functions
suspend fun Api.gameObject(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameObject>,
) = get(
    url = "game-object/$id",
    errorContext = "load object",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameObjects(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameObject>>,
) = get(
    url = "game-object",
    errorContext = "load objects",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createGameObject(
    gameObject: GameObject,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameObject>,
) = post(
    url = "game-object",
    body = gameObject,
    errorContext = "create object",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateGameObject(
    id: String,
    gameObject: GameObject,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameObject>,
) = post(
    url = "game-object/$id",
    body = gameObject,
    errorContext = "update object",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameObject(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-object/$id/delete",
    errorContext = "delete object",
    onError = onError,
    onSuccess = onSuccess
)

// GameMusic API functions
suspend fun Api.gameMusic(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameMusic>,
) = get(
    url = "game-music/$id",
    errorContext = "load music",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameMusics(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameMusic>>,
) = get(
    url = "game-music",
    errorContext = "load music",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createGameMusic(
    gameMusic: GameMusic,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameMusic>,
) = post(
    url = "game-music",
    body = gameMusic,
    errorContext = "create music",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateGameMusic(
    id: String,
    gameMusic: GameMusic,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameMusic>,
) = post(
    url = "game-music/$id",
    body = gameMusic,
    errorContext = "update music",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameMusic(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-music/$id/delete",
    errorContext = "delete music",
    onError = onError,
    onSuccess = onSuccess
)

// GameDiscussion API functions
suspend fun Api.gameSceneDiscussions(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameDiscussionExtended>>,
) = get(
    url = "game-scene/$id/discussions",
    errorContext = "load discussions",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameDiscussion(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameDiscussionExtended>,
) = get(
    url = "game-discussion/$id",
    errorContext = "load discussion",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createGameDiscussion(
    sceneId: String,
    discussion: GameDiscussion,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameDiscussion>,
) = post(
    url = "game-scene/$sceneId/discussion",
    body = discussion,
    errorContext = "create discussion",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.commentOnGameDiscussion(
    id: String,
    comment: Comment,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Comment>,
) = post(
    url = "game-discussion/$id/comment",
    body = comment,
    errorContext = "add comment",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameDiscussion(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-discussion/$id/delete",
    errorContext = "delete discussion",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.resolveGameDiscussion(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameDiscussion>,
) = post(
    url = "game-discussion/$id/resolve",
    errorContext = "resolve discussion",
    onError = onError,
    onSuccess = onSuccess
)
