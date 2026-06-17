package app.cards

import LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import api
import app.AppStyles
import app.ailaai.api.card
import app.ailaai.api.categories
import app.ailaai.api.deleteCard
import app.ailaai.api.updateCard
import app.components.FlexInput
import app.dialog.ChoosePhotoDialogControl
import app.dialog.activityDialog
import app.dialog.additionalPhotosDialog
import app.dialog.cardLevelDialog
import app.dialog.cardSizeDialog
import app.dialog.categoryDialog
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.selectCardDialog
import app.dialog.selectGroupDialog
import app.dialog.setLocationDialog
import app.menu.InlineMenu
import app.menu.Menu
import app.messaages.inList
import appString
import appText
import application
import com.queatz.db.Card
import com.queatz.db.Pay
import com.queatz.db.PayFrequency
import com.queatz.db.asGeo
import focusable
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import qr
import r
import saves
import webBaseUrl

@Composable
fun ExplorePageMenu(
    card: Card,
    menuTarget: DOMRect,
    choosePhotoDialog: ChoosePhotoDialogControl,
    onCard: (Card) -> Unit,
    onCardUpdated: (Card) -> Unit,
    onCardDeleted: (Card) -> Unit,
    onDismiss: () -> Unit,
    scope: CoroutineScope,
) {
    val configuration = LocalConfiguration.current
    val me by application.me.collectAsState()
    val inAPage = appString { inAPage }
    val titleString = appString { title }
    val renameString = appString { rename }
    val categoryString = appString { category }
    val hintString = appString { hint }
    val updateString = appString { update }
    val activityString = appString { activity }

    fun rename() {
        scope.launch {
            val name = inputDialog(
                title = titleString,
                placeholder = "",
                confirmButton = renameString,
                defaultValue = card.name ?: ""
            )

            if (name == null) {
                return@launch
            }

            api.updateCard(
                id = card.id!!,
                card = Card(name = name)
            ) {
                onCardUpdated(it)
            }
        }
    }

    fun rehint() {
        scope.launch {
            val hintValue = inputDialog(
                title = hintString,
                placeholder = "",
                confirmButton = updateString,
                defaultValue = card.location ?: ""
            )

            if (hintValue == null) {
                return@launch
            }

            api.updateCard(
                id = card.id!!,
                card = Card(location = hintValue)
            ) {
                onCardUpdated(it)
            }
        }
    }

    fun moveToGroup(groupId: String) {
        scope.launch {
            api.updateCard(
                id = card.id!!,
                card = Card(
                    offline = false,
                    parent = null,
                    group = groupId,
                    equipped = false,
                    geo = null
                )
            ) {
                onCardUpdated(it)
            }
        }
    }

    fun moveToPage(cardId: String) {
        scope.launch {
            api.updateCard(
                id = card.id!!,
                card = Card(
                    offline = false,
                    parent = cardId,
                    group = null,
                    equipped = false,
                    geo = null
                )
            ) {
                onCardUpdated(it)
            }
        }
    }

    fun moveToGroup() {
        scope.launch {
            val group = selectGroupDialog(configuration)
            if (group != null) {
                moveToGroup(group.group!!.id!!)
            }
        }
    }

    fun moveToPage() {
        scope.launch {
            selectCardDialog(
                configuration = configuration,
                title = inAPage
            ) {
                it?.id?.let { id -> moveToPage(id) }
            }
        }
    }

    fun setCategory() {
        scope.launch {
            api.categories(
                geo = application.me.value?.geo?.asGeo() ?: return@launch
            ) { categoryList ->
                val selectedCategory = categoryDialog(
                    categories = categoryList,
                    category = card.categories?.firstOrNull(),
                )

                if (selectedCategory != null) {
                    api.updateCard(
                        id = card.id!!,
                        card = Card(categories = selectedCategory.inList())
                    ) {
                        onCardUpdated(it)
                    }
                }
            }
        }
    }

    Menu(onDismiss, menuTarget) {
        val isSaved = saves.cards.value.any { it.id == card.id }

        item(appString { openInNewTab }, icon = "open_in_new") {
            window.open("/page/${card.id}", target = "_blank")
        }

        item(if (isSaved) appString { unsave } else appString { save }) {
            scope.launch {
                if (isSaved) {
                    saves.unsave(card.id!!)
                } else {
                    saves.save(card.id!!)
                }
            }
        }

        item(renameString) {
            rename()
        }

        item(categoryString) {
            setCategory()
        }

        item(activityString) {
            scope.launch {
                activityDialog(card) { updated ->
                    onCardUpdated(updated)
                }
            }
        }

        item(hintString) {
            rehint()
        }

        val location = appString { location }
        val close = appString { close }

        item(location) {
            scope.launch {
                dialog(
                    title = location,
                    confirmButton = null,
                    cancelButton = close
                ) {
                    InlineMenu({
                        it(true)
                    }) {
                        item(appString { onProfile }, selected = card.equipped == true, icon = "account_circle") {
                            scope.launch {
                                api.updateCard(
                                    id = card.id!!,
                                    card = Card(offline = false, parent = null, equipped = true, geo = null)
                                ) {
                                    onCardUpdated(it)
                                }
                            }
                        }
                        item(
                            appString { inAGroup },
                            selected = card.parent == null && card.offline != true && card.equipped != true && card.group != null,
                            icon = "group"
                        ) {
                            moveToGroup()
                        }
                        item(
                            appString { atALocation },
                            selected = card.parent == null && card.offline != true && card.equipped != true && card.geo != null,
                            icon = "location_on"
                        ) {
                            scope.launch {
                                val initialGeo = card.geo?.asGeo()
                                val geo = setLocationDialog(
                                    initialGeo = initialGeo,
                                    onRemoveLocation = {
                                        scope.launch {
                                            api.updateCard(
                                                id = card.id!!,
                                                card = Card(offline = false, parent = null, equipped = false, geo = null)
                                            ) {
                                                onCardUpdated(it)
                                            }
                                        }
                                    }
                                )
                                if (geo != null) {
                                    api.updateCard(
                                        id = card.id!!,
                                        card = Card(
                                            offline = false,
                                            parent = null,
                                            equipped = false,
                                            geo = listOf(geo.latitude, geo.longitude)
                                        )
                                    ) {
                                        onCardUpdated(it)
                                    }
                                }
                            }
                        }
                        item(inAPage, selected = card.parent != null, icon = "description") {
                            moveToPage()
                        }
                        item(appString { offline }, selected = card.offline == true) {
                            scope.launch {
                                api.updateCard(
                                    id = card.id!!,
                                    card = Card(offline = true, parent = null, equipped = false, geo = null)
                                ) {
                                    onCardUpdated(it)
                                }
                            }
                        }
                        item(
                            appString { none },
                            selected = card.parent == null && card.group == null && card.offline != true && card.equipped != true && card.geo == null
                        ) {
                            scope.launch {
                                api.updateCard(
                                    id = card.id!!,
                                    card = Card(offline = false, parent = null, equipped = false, geo = null)
                                ) {
                                    onCardUpdated(it)
                                }
                            }
                        }
                    }
                }
            }
        }

        item(appString { if (card.pay == null) addPay else changePay }) {
            scope.launch {
                val updatedPay = card.pay ?: Pay(pay = "")

                val result = dialog(
                    title = application.appString { price },
                    confirmButton = application.appString { update }
                ) { resolve ->
                    var pay by remember {
                        mutableStateOf(updatedPay.pay ?: "")
                    }

                    var payFrequency by remember {
                        mutableStateOf(updatedPay.frequency)
                    }

                    LaunchedEffect(pay, payFrequency) {
                        updatedPay.pay = pay
                        updatedPay.frequency = payFrequency
                    }

                    FlexInput(
                        value = pay,
                        onChange = { pay = it },
                        singleLine = true,
                        autoFocus = true,
                        selectAll = true,
                        styles = {
                            width(100.percent)
                            marginBottom(1.r)
                        },
                        onSubmit = {
                            resolve(true)
                            true
                        }
                    )

                    PayFrequency.entries.forEach { frequency ->
                        Div({
                            classes(
                                listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                            )

                            if (payFrequency == frequency) {
                                classes(AppStyles.groupItemSelected)
                            }

                            onClick {
                                payFrequency = if (payFrequency == frequency) null else frequency
                            }

                            focusable()
                        }) {
                            Div {
                                Div({
                                    classes(AppStyles.groupItemName)
                                }) {
                                    Text(frequency.appString)
                                }
                            }
                        }
                    }
                }

                if (result == true) {
                    api.updateCard(
                        id = card.id!!,
                        card = Card(pay = updatedPay)
                    ) {
                        onCardUpdated(it)
                    }
                }
            }
        }

        item(appString { choosePhoto }) {
            choosePhotoDialog.launch { photo, _, _ ->
                api.updateCard(
                    id = card.id!!,
                    card = Card(photo = photo)
                ) {
                    onCardUpdated(it)
                }
            }
        }

        item(appString { additionalPhotos }) {
            scope.launch {
                additionalPhotosDialog(card, onCardUpdated)
            }
        }

        if (me?.id == card.person) {
            item(appString { level }) {
                scope.launch {
                    cardLevelDialog(
                        card = card,
                        onCardUpdated = onCardUpdated
                    )
                }
            }

            item(appString { pageSize }) {
                scope.launch {
                    cardSizeDialog(
                        card = card,
                        onCardUpdated = onCardUpdated
                    )
                }
            }
        }

        if (card.parent != null) {
            item(appString { openEnclosingCard }) {
                scope.launch {
                    api.card(
                        id = card.parent!!
                    ) {
                        onCard(it)
                    }
                }
            }
        }

        item(appString { qrCode }) {
            scope.launch {
                dialog(
                    title = "",
                    cancelButton = null
                ) {
                    val qrCode = remember {
                        "$webBaseUrl/page/${card.id!!}".qr
                    }
                    Img(src = qrCode) {
                        style {
                            borderRadius(1.r)
                        }
                    }
                }
            }
        }

        item(appString { delete }) {
            scope.launch {
                val result = dialog(
                    title = application.appString { deleteThisPage },
                    confirmButton = application.appString { yesDelete }
                ) {
                    appText { youCannotUndoThis }
                }

                if (result == true) {
                    api.deleteCard(
                        id = card.id!!
                    ) {
                        onCardDeleted(card)
                    }
                }
            }
        }
    }
}
