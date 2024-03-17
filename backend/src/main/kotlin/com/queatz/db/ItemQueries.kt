package com.queatz.db

import com.queatz.plugins.defaultInventoriesNearbyMaxDistanceInMeters

fun Db.myItems(person: String, personInventory: String) = query(
    ItemExtended::class,
    """
        for x in `${Item::class.collection()}`
            filter x.${f(Item::creator)} == @person
            sort x.${f(Item::createdAt)} desc
            return {
                ${f(ItemExtended::item)}: x,
                ${f(ItemExtended::inventory)}: sum(
                    for inventoryItem in `${InventoryItem::class.collection()}`
                        filter inventoryItem.${f(InventoryItem::item)} == x._key 
                            and inventoryItem.${f(InventoryItem::inventory)} == @inventory
                            and (inventoryItem.${f(InventoryItem::expiresAt)} == null or DATE_ISO8601(inventoryItem.${f(InventoryItem::expiresAt)}) > DATE_ISO8601(DATE_NOW()))
                        return inventoryItem.${f(InventoryItem::quantity)} || 0.0 
                ),
                ${f(ItemExtended::circulating)}: sum(
                    for inventoryItem in `${InventoryItem::class.collection()}`
                        filter inventoryItem.${f(InventoryItem::item)} == x._key 
                            and inventoryItem.${f(InventoryItem::inventory)} != @inventory
                            and (inventoryItem.${f(InventoryItem::expiresAt)} == null or DATE_ISO8601(inventoryItem.${f(InventoryItem::expiresAt)}) > DATE_ISO8601(DATE_NOW()))
                        return inventoryItem.${f(InventoryItem::quantity)} || 0.0 
                ),
                ${f(ItemExtended::expired)}: sum(
                    for inventoryItem in `${InventoryItem::class.collection()}`
                        filter inventoryItem.${f(InventoryItem::item)} == x._key 
                            and (inventoryItem.${f(InventoryItem::expiresAt)} != null and DATE_ISO8601(inventoryItem.${f(InventoryItem::expiresAt)}) <= DATE_ISO8601(DATE_NOW()))
                        return inventoryItem.${f(InventoryItem::quantity)} || 0.0 
                )
            }
    """.trimIndent(),
    mapOf(
        "person" to person,
        "inventory" to personInventory
    )
)

fun Db.inventoryItems(inventory: String) = query(
    InventoryItemExtended::class,
    """
        for x in `${InventoryItem::class.collection()}`
            filter x.${f(InventoryItem::inventory)} == @inventory
            sort x.${f(InventoryItem::createdAt)} desc
            return ${inventoryItemExtended()}
    """.trimIndent(),
    mapOf(
        "inventory" to inventory
    )
)

fun Db.inventoryOfPerson(person: String) = one(
    Inventory::class,
    """
        upsert { ${f(Inventory::person)}: @person }
        insert {
            ${f(Inventory::person)}: @person,
            ${f(Inventory::createdAt)}: DATE_ISO8601(DATE_NOW())
        }
        update {}
        in @@collection
        return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)!!

fun Db.inventoryOfGeo(geo: List<Double>, nearbyMaxDistance: Double = 1.0) = one(
    Inventory::class,
    """
        for x in @@collection
            filter x.geo != null
            let d = distance(x.${f(Inventory::geo)}[0], x.${f(Inventory::geo)}[1], @geo[0], @geo[1])
            filter d < $nearbyMaxDistance
            sort d
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "geo" to geo
    )
)

fun Db.inventoriesNearGeo(
    geo: List<Double>,
    offset: Int = 0,
    limit: Int = 20,
    nearbyMaxDistance: Double = defaultInventoriesNearbyMaxDistanceInMeters
) = list(
    Inventory::class,
    """
        for x in @@collection
            filter x.geo != null
            let d = distance(x.${f(Inventory::geo)}[0], x.${f(Inventory::geo)}[1], @geo[0], @geo[1])
            filter d < $nearbyMaxDistance
            sort d
            limit @offset, @limit
            return x
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "offset" to offset,
        "limit" to limit,
    )
)

fun Db.inventoryItemExtended(inventoryItemVar: String = "x") = """
    {
        ${f(InventoryItemExtended::inventoryItem)}: $inventoryItemVar,
        ${f(InventoryItemExtended::item)}: first(
            for item in `${Item::class.collection()}`
                filter item._key == $inventoryItemVar.${f(InventoryItem::item)}
                return item
        )
    }
""".trimIndent()
