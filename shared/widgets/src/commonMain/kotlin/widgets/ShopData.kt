package com.queatz.widgets.widgets

import kotlinx.serialization.Serializable

@Serializable
data class ShopData(
    var items: List<ShopItemData>? = null
)

@Serializable
data class ShopItemData(
    var item: String? = null,
    var price: String? = null
)
