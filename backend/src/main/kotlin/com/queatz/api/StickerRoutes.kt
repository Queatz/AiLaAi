package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.*
import com.queatz.receiveFile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.stickerRoutes() {
    authenticate(optional = true) {
        get("/sticker-packs/{id}") {
            respond {
                db.stickerPackWithStickers(parameter("id")) ?: HttpStatusCode.NotFound
            }
        }
    }

    get("/sticker/{id}") {
        respond {
            db.document(
                Sticker::class,
                parameter("id")
            ) ?: HttpStatusCode.NotFound
        }
    }

    authenticate {
        get("/me/sticker-packs") {
            respond {
                db.myStickerPacks(me.id!!)
            }
        }

        get("/sticker-packs") {
            respond {
                db.stickerPacks(me.id!!)
            }
        }

        post("/sticker-packs/{id}/save") {
            respond {
                db.saveStickerPack(me.id!!, parameter("id"))
                HttpStatusCode.NoContent
            }
        }

        post("/sticker-packs/{id}/unsave") {
            respond {
                db.unsaveStickerPack(me.id!!, parameter("id"))
                HttpStatusCode.NoContent
            }
        }

        post("/sticker-packs") {
            respond {
                val stickerPack = StickerPack().apply {
                    val newStickerPack = call.receive<StickerPack>()
                    name = newStickerPack.name
                    description = newStickerPack.description
                    person = me.id!!
                }.let {
                    db.insert(it)
                }
                db.stickerPackWithStickers(stickerPack.id!!)!!
            }
        }

        post("/sticker-packs/{id}") {
            respond {
                val stickerPack = myStickerPack(parameter("id"), me) ?: return@respond HttpStatusCode.NotFound

                call.receive<StickerPack>().let { update ->
                    if (update.name != null) {
                        stickerPack.name = update.name
                    }
                    if (update.description != null) {
                        stickerPack.description = update.description
                    }
                    if (update.active != null) {
                        stickerPack.active = update.active
                    }
                }

                db.update(stickerPack)
                db.stickerPackWithStickers(stickerPack.id!!)!!
            }
        }

        get("/sticker-packs/{id}/stickers") {
            respond {
                val stickerPack = db.document(
                    StickerPack::class,
                    parameter("id")
                )?.takeIf {
                    it.person == meOrNull?.id || it.active == true
                } ?: return@respond HttpStatusCode.NotFound.description("Sticker pack not found")

                db.stickers(stickerPack.id!!)
            }
        }

        post("/sticker-packs/{id}/stickers") {
            respond {
                val stickerPack = myStickerPack(parameter("id"), me) ?: return@respond HttpStatusCode.NotFound.description("Sticker pack not found")

                var sticker: Sticker? = null

                call.receiveFile("photo", "sticker-pack-${stickerPack.id}") { photoUrl, params ->
                    val newSticker = params["sticker"]?.let { json.decodeFromString<Sticker>(it) }
                    sticker = Sticker(
                        photo = photoUrl,
                        pack = stickerPack.id!!,
                        name = newSticker?.name,
                        message = newSticker?.message
                    )
                }

                db.insert(sticker ?: return@respond HttpStatusCode.BadRequest.description("Sticker could not be created"))
            }
        }

        post("/sticker/{id}") {
            respond {
                val sticker = mySticker(parameter("id"), me) ?: return@respond HttpStatusCode.NotFound

                call.receive<Sticker>().let { update ->
                    if (update.name != null) {
                        sticker.name = update.name
                    }
                    if (update.message != null) {
                        sticker.message = update.message
                    }
                    if (update.photo != null) {
                        sticker.photo = update.photo
                    }
                }

                db.update(sticker)
            }
        }

        post("/sticker/{id}/delete") {
            respond {
                val sticker = mySticker(parameter("id"), me) ?: return@respond HttpStatusCode.NotFound

                db.delete(sticker)

                HttpStatusCode.NoContent
            }
        }

        post("/sticker/{id}/delete") {
            respond {
                val sticker = mySticker(parameter("id"), me) ?: return@respond HttpStatusCode.NotFound

                db.delete(sticker)

                HttpStatusCode.NoContent
            }
        }

        post("/sticker-pack/{id}/delete") {
            respond {
                val stickerPack = myStickerPack(parameter("id"), me) ?: return@respond HttpStatusCode.NotFound

                db.stickers(stickerPack.id!!).forEach {
                    db.delete(it)
                }
                db.delete(stickerPack)

                HttpStatusCode.NoContent
            }
        }
    }
}

private fun mySticker(stickerId: String, me: Person): Sticker? {
    val sticker = db.document(Sticker::class, stickerId) ?: return null
    val stickerPack = db.document(StickerPack::class, sticker.pack!!) ?: return null

    if (stickerPack.person != me.id) {
        return null
    }

    return sticker
}

private fun myStickerPack(stickerPackId: String, me: Person): StickerPack? {
    val stickerPack = db.document(StickerPack::class, stickerPackId) ?: return null

    if (stickerPack.person != me.id) {
        return null
    }

    return stickerPack
}
