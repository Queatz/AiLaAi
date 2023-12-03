package app.widget

import androidx.compose.runtime.*
import api
import app.ailaai.api.cardsCards
import app.ailaai.api.updateCard
import app.dialog.inputDialog
import appString
import application
import com.queatz.db.Card
import com.queatz.db.Widget
import com.queatz.widgets.widgets.ImpactEffortTableData
import com.queatz.widgets.widgets.ImpactEffortTablePoint
import components.getConversation
import isMine
import json
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import updateWidget
import widget

@Composable
fun ImpactEffortTable(widgetId: String) {
    val me by application.me.collectAsState()

    var widget by remember(widgetId) {
        mutableStateOf<Widget?>(null)
    }
    var cards by remember(widgetId) {
        mutableStateOf<List<Card>>(emptyList())
    }
    var sort by remember(widgetId) {
        mutableStateOf(0)
    }
    var desc by remember(widgetId) {
        mutableStateOf(false)
    }
    var data by remember(widgetId) {
        mutableStateOf<ImpactEffortTableData?>(null)
    }

    val priority by remember(cards, data) {
        val all = cards.mapNotNull {
            val impact = (data?.points?.get(it.id)?.impact ?: return@mapNotNull null).toFloat()
            val effort = ((data?.points?.get(it.id))?.effort ?: return@mapNotNull null).toFloat()
            1f - ((impact / effort) / 10f)
        }

        val min = all.minOrNull() ?: 0f
        val max = all.maxOrNull() ?: 1f

        // todo this isn't really mutable
        mutableStateOf(
            cards
                .associateBy { it.id!! }
                .mapValues {
                    val impact = (data?.points?.get(it.key)?.impact ?: return@mapValues null).toFloat()
                    val effort = ((data?.points?.get(it.key))?.effort ?: return@mapValues null).toFloat()
                    (1f - ((impact / effort) / 10f)).normalize(min, max)
                }
        )
    }
    val sorted by remember(priority, cards, sort, desc) {
        // todo this isn't really mutable
        mutableStateOf(
            when (sort) {
                0 -> cards.sortedBy { priority[it.id!!] ?: Float.MAX_VALUE }
                1 -> cards.sortedBy { data?.points?.get(it.id)?.impact ?: Int.MAX_VALUE }
                2 -> cards.sortedBy { data?.points?.get(it.id)?.effort ?: Int.MAX_VALUE }
                3 -> cards.sortedBy { it.name }
                4 -> cards.sortedBy { it.getConversation().message }
                else -> cards
            }.let { if (desc) it.asReversed() else it }
        )
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(widgetId) {
        // todo loading
        api.widget(widgetId) {
            it.data ?: return@widget

            widget = it

            data = json.decodeFromString<ImpactEffortTableData>(it.data!!)

            api.cardsCards(data?.card ?: return@widget) {
                cards = it
            }
        }
    }

    fun save(data: ImpactEffortTableData) {
        scope.launch {
            api.updateWidget(widgetId, Widget(data = json.encodeToString(data))) {

            }
        }
    }

    suspend fun saveConversation(card: Card, value: String) {
        val conversation = card.getConversation()
        conversation.message = value
        api.updateCard(card.id!!, Card(conversation = json.encodeToString(conversation))) {

        }
        api.cardsCards(data?.card ?: return) {
            cards = it
        }
    }

    suspend fun saveName(card: Card, value: String) {
        api.updateCard(card.id!!, Card(name = value)) {

        }
        api.cardsCards(data?.card ?: return) {
            cards = it
        }
    }

    Div(
        {
            style {
                overflowX("auto")
                width(100.percent)
            }
        }
    ) {
        Table({
            classes(WidgetStyles.table)
        }) {
            Thead {
                Tr {
                    Th({
                        if (sort == 0) {
                            classes(WidgetStyles.columnSelected)
                        }

                        classes(WidgetStyles.tableCenter)

                        onClick {
                            if (sort == 0) {
                                desc = !desc
                            } else {
                                sort = 0
                            }
                        }
                    }) {
                        Text("Priority")
                    }
                    Th({
                        if (sort == 1) {
                            classes(WidgetStyles.columnSelected)
                        }

                        classes(WidgetStyles.tableCenter)

                        onClick {
                            if (sort == 1) {
                                desc = !desc
                            } else {
                                sort = 1
                            }
                        }
                    }) {
                        Text("Impact")
                    }
                    Th({
                        if (sort == 2) {
                            classes(WidgetStyles.columnSelected)
                        }

                        classes(WidgetStyles.tableCenter)

                        onClick {
                            if (sort == 2) {
                                desc = !desc
                            } else {
                                sort = 2
                            }
                        }
                    }) {
                        Text("Effort")
                    }
                    Th({
                        if (sort == 3) {
                            classes(WidgetStyles.columnSelected)
                        }

                        classes(WidgetStyles.tableCenter)

                        onClick {
                            if (sort == 3) {
                                desc = !desc
                            } else {
                                sort = 3
                            }
                        }
                    }) {
                        Text("Name")
                    }
                    Th({
                        if (sort == 4) {
                            classes(WidgetStyles.columnSelected)
                        }

                        onClick {
                            if (sort == 4) {
                                desc = !desc
                            } else {
                                sort = 4
                            }
                        }
                    }) {
                        Text("Details")
                    }
                }
            }
            Tbody {
                sorted.forEach { card ->
                    key(card.id!!) {
                        Tr {
                            Td({
                                classes(WidgetStyles.tableCenter)

                                style {

                                    if (priority[card.id!!] == null) {
                                        opacity(.25f)
                                    }
                                }
                            }) {
                                B {
                                    Text("P${priority[card.id!!]?.times(10f)?.toInt()?.toString() ?: "?"}")
                                }
                            }
                            Td({
                                classes(WidgetStyles.tableCenter)

                                style {
                                    if (me?.id == widget?.person) {
                                        cursor("pointer")
                                    }

                                    if (data?.points?.get(card.id!!)?.impact == null) {
                                        opacity(.25f)
                                    }
                                }

                                if (me?.id == widget?.person) {
                                    onClick {
                                        scope.launch {
                                            val result = inputDialog(
                                                "Impact",
                                                placeholder = "1-10",
                                                confirmButton = application.appString { update },
                                                defaultValue = data?.points?.get(card.id!!)?.impact?.toString() ?: ""
                                            )

                                            result?.let { value ->
                                                data = data!!.copy(
                                                    points = (data?.points?.toMutableMap() ?: mutableMapOf()).apply {
                                                        put(
                                                            card.id!!,
                                                            getOrElse(card.id!!) { ImpactEffortTablePoint() }.copy(
                                                                impact = value.toIntOrNull()?.coerceIn(1, 10)
                                                            )
                                                        )
                                                    }.toMap()
                                                )
                                                save(data!!)
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text(data?.points?.get(card.id!!)?.impact?.toString() ?: "None")
                            }
                            Td({
                                classes(WidgetStyles.tableCenter)

                                style {
                                    if (me?.id == widget?.person) {
                                        cursor("pointer")
                                    }

                                    if (data?.points?.get(card.id!!)?.effort == null) {
                                        opacity(.25f)
                                    }
                                }

                                if (me?.id == widget?.person) {
                                    onClick {
                                        scope.launch {
                                            val result = inputDialog(
                                                "Effort",
                                                placeholder = "1-10",
                                                confirmButton = application.appString { update },
                                                defaultValue = data?.points?.get(card.id!!)?.effort?.toString() ?: ""
                                            )

                                            result?.let { value ->
                                                data = data!!.copy(
                                                    points = (data?.points?.toMutableMap() ?: mutableMapOf()).apply {
                                                        put(
                                                            card.id!!,
                                                            getOrElse(card.id!!) { ImpactEffortTablePoint() }.copy(
                                                                effort = value.toIntOrNull()?.coerceIn(1, 10)
                                                            )
                                                        )
                                                    }.toMap()
                                                )
                                                save(data!!)
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text(data?.points?.get(card.id!!)?.effort?.toString() ?: "None")
                            }
                            Td(
                                {
                                    classes(WidgetStyles.tableCenter)

                                    if (card.isMine(me?.id)) {
                                        style {
                                            cursor("pointer")
                                        }

                                        onClick {
                                            scope.launch {
                                                val result = inputDialog(
                                                    application.appString { rename },
                                                    confirmButton = application.appString { update },
                                                    defaultValue = card.name ?: ""
                                                )

                                                if (result != null) {
                                                    saveName(card, result)
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(card.name ?: appString { newCard })
                            }
                            Td({
                                if (card.isMine(me?.id)) {
                                    style {
                                        cursor("pointer")
                                    }

                                    onClick {
                                        scope.launch {
                                            val result = inputDialog(
                                                application.appString { details },
                                                singleLine = false,
                                                confirmButton = application.appString { update },
                                                defaultValue = card.getConversation().message
                                            )

                                            if (result != null) {
                                                saveConversation(card, result)
                                            }
                                        }
                                    }
                                }
                            }) {
                                Text(card.getConversation().message)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Float.normalize(min: Float, max: Float) = (this - min) / (max - min)
