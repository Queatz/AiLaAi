import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.*
import io.ktor.http.*

suspend fun Api.createItem(
    item: Item,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Item> = {}
) = post("items", item, errorContext = "create item", onError = onError, onSuccess = onSuccess)

suspend fun Api.mintItem(
    item: String,
    body: MintItemBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<InventoryItem> = {}
) = post("items/$item/mint", body, errorContext = "mint item", onError = onError, onSuccess = onSuccess)

suspend fun Api.dropItem(
    inventoryItem: String,
    body: DropItemBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("inventory/$inventoryItem/drop", body, errorContext = "drop item", onError = onError, onSuccess = onSuccess)

suspend fun Api.equipItem(
    inventoryItem: String,
    body: EquipItemBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("inventory/$inventoryItem/equip", body, errorContext = "equip item", onError = onError, onSuccess = onSuccess)

suspend fun Api.unequipItem(
    inventoryItem: String,
    body: UnequipItemBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("inventory/$inventoryItem/unequip", body, errorContext = "unequip item", onError = onError, onSuccess = onSuccess)

suspend fun Api.myItems(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<ItemExtended>>
) = get("items", errorContext = "load items", onError = onError, onSuccess = onSuccess)

suspend fun Api.myInventory(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<InventoryItemExtended>>
) = get("me/inventory", errorContext = "load inventory", onError = onError, onSuccess = onSuccess)

suspend fun Api.inventoriesNear(
    geo: Geo,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Inventory>>
) = get(
    "inventory/explore", mapOf(
        "geo" to geo.toString()
    ),
    errorContext = "load inventories",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.inventory(
    inventory: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<InventoryItemExtended>>
) = get(
    "inventory/$inventory",
    errorContext = "load inventory",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.takeInventory(
    inventory: String,
    items: List<TakeInventoryItem>,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    "inventory/$inventory/take",
    TakeInventoryBody(items),
    errorContext = "take items",
    onError = onError,
    onSuccess = onSuccess
)
