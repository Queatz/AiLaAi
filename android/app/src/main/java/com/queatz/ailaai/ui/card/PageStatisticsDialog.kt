package com.queatz.ailaai.ui.card

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.cardVisits
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.formatDate
import com.queatz.ailaai.extensions.formatDateStamp
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.startOfDay
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.CardVisit
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageStatisticsDialog(
    card: Card,
    onDismissRequest: () -> Unit,
) {
    val since by rememberStateOf(Clock.System.now().startOfDay() - 6.days)
    var visits by rememberStateOf<List<CardVisit>>(emptyList())
    var isLoading by rememberStateOf(false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        api.cardVisits(card.id!!, since) {
            visits = it
        }
        isLoading = false
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                var myVisits by rememberStateOf(false)
                var anonymousVisits by rememberStateOf(false)
                var othersVisits by rememberStateOf(false)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.statistics),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                    )
                }
                if (isLoading) {
                    Loading()
                } else {
                    Text(pluralStringResource(R.plurals.x_visits_since_x, visits.size, visits.size, since.formatDate()))

                    // todo checkboxes for include My visits, Anonymous visits only, Hi Town user visits only here

                    FlowRow {
                        Check(myVisits, { myVisits = it }) {
                            Text(stringResource(R.string.me))
                        }
                        Check(anonymousVisits, { anonymousVisits = it }) {
                            Text(stringResource(R.string.anonymous))
                        }
                        Check(othersVisits, { othersVisits = it }) {
                            Text(stringResource(R.string.others))
                        }
                    }

                    val allBarData = remember(visits) {
                        visits.groupBy { it.createdAt!!.startOfDay() }
                    }

                    val barData = remember(
                        allBarData,
                        myVisits,
                        anonymousVisits,
                        othersVisits
                    ) {
                        allBarData.let {
                            if (!myVisits && !anonymousVisits && !othersVisits) {
                                it
                            } else {
                                it.mapValues { (key, value) ->
                                    value.filter {
                                        (it.isOwner == true && myVisits) ||
                                        (it.isOwner != true && it.isWild != true && othersVisits) ||
                                        (it.isOwner != true && it.isWild == true && anonymousVisits)
                                    }
                                }
                            }
                        }
                    }

                    val max = (allBarData.entries.maxOfOrNull { (_, value) -> value.size } ?: 0) / 5 * 5 + 10

                    val bars = remember(since) {
                        val now = Clock.System.now().startOfDay()
                        (0..<7).map {
                            now - it.days
                        }
                    }.asReversed()

                    if (bars.isNotEmpty()) {
                        ColumnChart(
                            modifier = Modifier.fillMaxWidth().height(240.dp).padding(vertical = 1.pad),
                            data =
                            bars.map {
                                Bars(
                                    label = it.formatDateStamp(),
                                    values = listOf(
                                        Bars.Data(
                                            value = (barData[it]?.size?.toDouble() ?: 0.0).coerceAtLeast(0.0001),
                                            color = SolidColor(MaterialTheme.colorScheme.primary)
                                        )
                                    )
                                )
                            },
                            barProperties = BarProperties(
                                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
                                spacing = 1.pad
                            ),
                            labelProperties = LabelProperties(
                                enabled = true,
                                textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                labels = bars.map {
                                    it.formatDateStamp()
                                },
                            ),
                            labelHelperProperties = LabelHelperProperties(
                                enabled = false
                            ),
                            gridProperties = GridProperties(
                                enabled = false
                            ),
                            popupProperties = PopupProperties(
                                enabled = true,
                                contentBuilder = {
                                    it.toInt().format()
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            ),
                            indicatorProperties = HorizontalIndicatorProperties(
                                enabled = true,
                                textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                count = IndicatorCount.CountBased(if (max < 20) 2 else 5), // todo: use step based
                                padding = 1.pad,
                                contentBuilder = {
                                    it.toInt().format()
                                }
                            ),
                            animationMode = AnimationMode.Together(),
                            animationSpec = tween(150),
                            maxValue = max.toDouble()
                        )
                    }
                }
            },
            actions = {
                Button(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
