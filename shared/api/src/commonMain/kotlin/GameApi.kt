package app.ailaai.api

import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.Comment
import com.queatz.db.GameDiscussion
import com.queatz.db.GameDiscussionExtended
import com.queatz.db.GameScene
import com.queatz.db.GameTile
import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import io.ktor.http.HttpStatusCode

// GameScene API functions
suspend fun Api.gameScene(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameScene>,
) = get(
    url = "game-scene/$id",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameSceneByUrl(
    url: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameScene>,
) = get(
    url = "game-scene/url/$url",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameScene(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-scene/$id/delete",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameScenes(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameScene>>,
) = get(
    url = "game-scene",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameTiles(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameTile>>,
) = get(
    url = "game-tile",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameTile(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-tile/$id/delete",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameObjects(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameObject>>,
) = get(
    url = "game-object",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameObject(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-object/$id/delete",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameMusics(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<GameMusic>>,
) = get(
    url = "game-music",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameMusic(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-music/$id/delete",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.gameDiscussion(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameDiscussionExtended>,
) = get(
    url = "game-discussion/$id",
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
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteGameDiscussion(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    url = "game-discussion/$id/delete",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.resolveGameDiscussion(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<GameDiscussion>,
) = post(
    url = "game-discussion/$id/resolve",
    onError = onError,
    onSuccess = onSuccess
)
