package com.queatz.db

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
            return {
                ${f(InventoryItemExtended::inventoryItem)}: x,
                ${f(InventoryItemExtended::item)}: first(for item in `${Item::class.collection()}` filter item._key == x.${f(InventoryItem::item)} return item)
            }
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
