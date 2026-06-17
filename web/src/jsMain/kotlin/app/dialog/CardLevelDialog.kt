package app.dialog

import api
import app.AppStyles
import app.ailaai.api.card
import app.ailaai.api.downgradeCard
import app.ailaai.api.downgradeCardDetails
import app.ailaai.api.upgradeCard
import app.ailaai.api.upgradeCardDetails
import appString
import application
import com.queatz.db.Card
import com.queatz.db.CardDowngradeBody
import com.queatz.db.CardDowngradeDetails
import com.queatz.db.CardUpgradeBody
import com.queatz.db.CardUpgradeDetails
import focusable
import format
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

private sealed class CardLevelAction {
    data class Upgrade(val level: Int) : CardLevelAction()
    data class Downgrade(val level: Int) : CardLevelAction()
}

suspend fun cardLevelDialog(
    card: Card,
    onCardUpdated: (Card) -> Unit,
) {
    val currentLevel = card.level ?: 0

    var upgradeDetails: CardUpgradeDetails? = null
    var downgradeDetails: CardDowngradeDetails? = null

    api.upgradeCardDetails(
        id = card.id!!
    ) {
        upgradeDetails = it
    }

    if (currentLevel > 0) {
        api.downgradeCardDetails(
            id = card.id!!
        ) {
            downgradeDetails = it
        }
    }

    var chosenAction: CardLevelAction? = null

    val confirmed = dialog(
        title = application.appString { level },
        confirmButton = null,
        cancelButton = application.appString { close }
    ) { resolve ->
        upgradeDetails?.let { upgrade ->
            val levelString = appString { levelX }.format(upgrade.level.toString())
            val pointsString = appString { xPoints }.format(upgrade.points.toString())
            val descriptionString = if (upgrade.available) {
                appString { upgradePageToLevelForPoints }.format(levelString, pointsString)
            } else {
                appString { upgradingPageToLevelRequiresPoints }.format(levelString, pointsString)
            }

            Div({
                classes(
                    listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                )

                style {
                    marginBottom(1.r)
                }

                if (upgrade.available) {
                    onClick {
                        chosenAction = CardLevelAction.Upgrade(level = upgrade.level)
                        resolve(true)
                    }
                    focusable()
                }
            }) {
                Div {
                    Div({
                        classes(AppStyles.groupItemName)
                    }) {
                        Text(appString { this.upgrade })
                    }
                    Div({
                        classes(AppStyles.groupItemMessage)
                    }) {
                        Text(descriptionString)
                    }
                }
            }
        }

        if (currentLevel > 0) {
            downgradeDetails?.let { downgrade ->
                val levelString = appString { levelX }.format(downgrade.level.toString())
                val pointsString = appString { xPoints }.format(downgrade.points.toString())
                val descriptionString = appString { downgradingPageToLevelRefundsPoints }.format(
                    levelString,
                    pointsString
                )

                Div({
                    classes(
                        listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                    )

                    onClick {
                        chosenAction = CardLevelAction.Downgrade(level = downgrade.level)
                        resolve(true)
                    }

                    focusable()
                }) {
                    Div {
                        Div({
                            classes(AppStyles.groupItemName)
                        }) {
                            Text(appString { this.downgrade })
                        }
                        Div({
                            classes(AppStyles.groupItemMessage)
                        }) {
                            Text(descriptionString)
                        }
                    }
                }
            }
        }
    }

    if (confirmed == true) {
        when (val action = chosenAction) {
            is CardLevelAction.Upgrade -> {
                api.upgradeCard(
                    id = card.id!!,
                    upgrade = CardUpgradeBody(level = action.level)
                ) {
                    api.card(
                        id = card.id!!
                    ) { updatedCard ->
                        onCardUpdated(updatedCard)
                    }
                }
            }
            is CardLevelAction.Downgrade -> {
                api.downgradeCard(
                    id = card.id!!,
                    downgrade = CardDowngradeBody(level = action.level)
                ) {
                    api.card(
                        id = card.id!!
                    ) { updatedCard ->
                        onCardUpdated(updatedCard)
                    }
                }
            }
            null -> Unit
        }
    }
}
