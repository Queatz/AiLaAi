package app.dialog

import application

suspend fun categoryDialog(
    categories: List<String>,
    category: String? = null
): String? = inputSelectDialog(
    confirmButton = application.appString { confirm },
    defaultValue = category.orEmpty(),
    items = categories
)
