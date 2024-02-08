package com.queatz.db

fun Db.trades(
    person: String,
    offset: Int = 0,
    limit: Int = 20
) = query(
    TradeExtended::class,
    """
        for x in `${Trade::class.collection()}`
            filter @person in x.${f(Trade::people)}
            sort x.${f(Trade::createdAt)} desc
            limit @offset, @limit
            return ${tradeExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person,
        "offset" to offset,
        "limit" to limit
    )
)

fun Db.trade(
    person: String,
    trade: String
) = query(
    TradeExtended::class,
    """
        for x in `${Trade::class.collection()}`
            filter x._key == @trade
                and @person in x.${f(Trade::people)}
            return ${tradeExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person,
        "trade" to trade
    )
).firstOrNull()

fun Db.activeTrades(
    person: String
) = query(
    TradeExtended::class,
    """
        for x in `${Trade::class.collection()}`
            filter @person in x.${f(Trade::people)}
                and x.${f(Trade::cancelledAt)} == null
                and x.${f(Trade::completedAt)} == null
            sort x.${f(Trade::createdAt)} desc
            return ${tradeExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

private fun Db.tradeExtended(tradeVar: String = "x") {
    """
        {
            ${f(TradeExtended::trade)}: $tradeVar,
            ${f(TradeExtended::inventoryItems)}: (
                for inventoryItem in ${InventoryItem::class.collection()}
                    filter inventoryItem._key in (
                        for member in $tradeVar.${f(Trade::members)}
                            for memberItem in member.${f(TradeMember::items)}
                                return memberItem.${f(TradeItem::inventoryItem)}
                    )
                    return ${inventoryItemExtended("inventoryItem")}
            ),
            ${f(TradeExtended::people)}: (
                for person in ${Person::class.collection()}
                    filter person._key in $tradeVar.${f(Trade::people)}
                    return person
            )
        }
    """.trimIndent()
}
