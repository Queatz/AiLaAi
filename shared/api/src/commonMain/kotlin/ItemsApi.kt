import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.*
import io.ktor.http.*

suspend fun Api.createItem(
    item: Item,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Item> = {}
) = post("items", item, onError = onError, onSuccess = onSuccess)

suspend fun Api.mintItem(
    item: String,
    body: MintItemBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<InventoryItem> = {}
) = post("items/$item/mint", body, onError = onError, onSuccess = onSuccess)

suspend fun Api.myItems(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<ItemExtended>>
) = get("items", onError = onError, onSuccess = onSuccess)

suspend fun Api.myInventory(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<InventoryItemExtended>>
) = get("me/inventory", onError = onError, onSuccess = onSuccess)
