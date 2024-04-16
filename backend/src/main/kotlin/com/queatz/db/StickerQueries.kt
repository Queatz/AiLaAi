package com.queatz.db

fun Db.withStickers(stickerPackVal: String) = """
    merge(
        $stickerPackVal,
        {
            ${f(StickerPack::stickers)}: (
                for sticker in ${Sticker::class.collection()}
                    filter sticker.${f(Sticker::pack)} == $stickerPackVal._key
                    sort sticker.${f(Sticker::name)}, sticker.${f(Sticker::createdAt)}
                    return sticker
                )
        }
    )
""".trimIndent()

fun Db.myStickerPacks(person: String) = list(
    StickerPack::class,
    """
        for pack in @@collection
            filter pack.${f(StickerPack::person)} == @person
            sort pack.${f(StickerPack::createdAt)} desc
            return ${withStickers("pack")}
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.stickerPacks(person: String)  = list(
    StickerPack::class,
    """
        for pack, save in outbound @person graph `${StickerPackSave::class.graph()}`
            sort save.${f(StickerPackSave::createdAt)} desc
            return ${withStickers("pack")}
    """,
    mapOf(
        "person" to person.asId(Person::class)
    )
)

fun Db.stickerPackWithStickers(stickerPack: String) = one(
    StickerPack::class,
    """
        for pack in @@collection
            filter pack._key == @pack
            return ${withStickers("pack")}
    """,
    mapOf(
        "pack" to stickerPack
    )
)

fun Db.saveStickerPack(person: String, stickerPack: String) = one(
    StickerPackSave::class,
    """
            upsert { _from: @person, _to: @pack }
                insert { _from: @person, _to: @pack, ${f(StickerPackSave::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { _from: @person, _to: @pack }
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "pack" to stickerPack.asId(StickerPack::class)
    )
)

fun Db.unsaveStickerPack(person: String, stickerPack: String) = query(
    StickerPackSave::class,
    """
            for x in `${StickerPackSave::class.collection()}`
                filter x._from == @person
                    and x._to == @pack
                remove x in `${StickerPackSave::class.collection()}`
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "pack" to stickerPack.asId(StickerPack::class)
    )
)

fun Db.stickers(stickerPack: String) = list(
    Sticker::class,
    """
        for x in @@collection
            filter x.${f(Sticker::pack)} == @pack
            return x
    """.trimIndent(),
    mapOf(
        "pack" to stickerPack
    )
)

