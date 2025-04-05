package app.dialog

import application

suspend fun categoryDialog(
    categories: List<String>
): String? = inputSelectDialog(
    confirmButton = application.appString { confirm },
    items = categories
)
