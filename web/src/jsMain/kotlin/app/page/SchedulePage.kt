package app.page

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import api
import app.FullPageLayout
import app.ailaai.api.newReminder
import app.ailaai.api.occurrences
import app.ailaai.api.updateReminderOccurrence
import app.components.FlexColumns
import app.dialog.inputDialog
import app.reminder.CalendarEvent
import app.reminder.Period
import app.reminder.ReminderPage
import app.reminder.formatSecondary
import app.reminder.formatTitle
import app.reminder.toEvents
import appString
import application
import bulletedString
import com.queatz.db.Reminder
import com.queatz.db.ReminderOccurrence
import components.IconButton
import format
import io.ktor.http.ContentType
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.toJSDate
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import lib.addDays
import lib.addMilliseconds
import lib.addMinutes
import lib.addMonths
import lib.addWeeks
import lib.addYears
import lib.isAfter
import lib.isBefore
import lib.isEqual
import lib.rawTimeZones
import lib.startOfDay
import lib.startOfMinute
import lib.startOfMonth
import lib.startOfWeek
import lib.startOfYear
import lib.systemTimezone
import notBlank
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import quantize
import r
import shadow
import toRem
import kotlin.js.Date
import kotlin.math.ceil
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class ScheduleViewType {
    Schedule,
    Calendar
}

enum class ScheduleView {
    Daily,
    Weekly,
    Monthly,
    Yearly
}

enum class ReminderEventType {
    Start,
    Occur,
    End
}

data class ReminderEvent(
    /**
     * The reminder that spawned this event
     */
    val reminder: Reminder,
    /**
     * The date of the event
     */
    val date: Date,
    /**
     * The type of event.
     */
    val event: ReminderEventType,
    /**
     * The `ReminderOccurrence` associated with this event, if any.
     */
    val occurrence: ReminderOccurrence?
)

val ReminderEvent.updateDate get() = occurrence?.occurrence ?: date.toKotlinInstant()

private data class ColumnInfo(
    val start: Date,
    val end: Date,
    val events: List<ReminderEvent>
)

@Serializable
data class ReminderDragData(
    val reminder: String,
    val occurrence: Instant
)

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun SchedulePage(
    view: ScheduleView,
    viewType: ScheduleViewType,
    reminder: Reminder?,
    search: String?,
    goToToday: Flow<Unit>,
    onReminder: (Reminder?) -> Unit,
    onUpdate: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    Style(SchedulePageStyles)

    val scope = rememberCoroutineScope()

    val changes = remember {
        MutableSharedFlow<Unit>()
    }

    var isLoading by remember(view) {
        mutableStateOf(true)
    }

    var events by remember(view) {
        mutableStateOf(emptyList<ReminderEvent>())
    }

    var offset by remember {
        mutableStateOf(startOfDay(Date()))
    }

    val shownEvents = remember(events, search) {
        if (search.isNullOrBlank()) {
            events
        } else {
            events.filter {
                it.reminder.title?.contains(search, ignoreCase = true) == true ||
                it.occurrence?.note?.contains(search, ignoreCase = true) == true ||
                it.reminder.note?.contains(search, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(viewType) {
        onReminder(null)
    }

    LaunchedEffect(view) {
        offset = startOfDay(Date())
        goToToday.collectLatest {
            offset = startOfDay(Date())
        }
    }

    if (reminder != null) {
        ReminderPage(
            reminder = reminder,
            onUpdate = { onUpdate(it) },
            onDelete = { onDelete(it) }
        )
        return
    }

    fun move(amount: Double) {
        offset = when (view) {
            ScheduleView.Daily -> addDays(offset, amount)
            ScheduleView.Weekly -> addWeeks(offset, amount)
            ScheduleView.Monthly -> addMonths(offset, amount)
            ScheduleView.Yearly -> addYears(offset, amount)
        }
    }

    val range = mapOf(
        ScheduleView.Daily to 7,
        ScheduleView.Weekly to 4,
        ScheduleView.Monthly to 3,
        ScheduleView.Yearly to 2
    )

    suspend fun reload() {
        val start = when (view) {
            ScheduleView.Daily -> startOfDay(offset)
            ScheduleView.Weekly -> startOfWeek(offset)
            ScheduleView.Monthly -> startOfMonth(offset)
            ScheduleView.Yearly -> startOfYear(offset)
        }

        api.occurrences(
            start = start.toKotlinInstant(),
            end = when (view) {
                ScheduleView.Daily -> addDays(start, range[ScheduleView.Daily]!!.toDouble())
                ScheduleView.Weekly -> addWeeks(start, range[ScheduleView.Weekly]!!.toDouble())
                ScheduleView.Monthly -> addMonths(start, range[ScheduleView.Monthly]!!.toDouble())
                ScheduleView.Yearly -> addYears(start, range[ScheduleView.Yearly]!!.toDouble())
            }.toKotlinInstant()
        ) {
            events = it.toEvents()
        }
        isLoading = false
    }

    fun moveOccurrence(dragData: ReminderDragData, toDate: Instant) {
        scope.launch {
            api.updateReminderOccurrence(
                id = dragData.reminder,
                occurrence = dragData.occurrence,
                update = ReminderOccurrence(
                    date = toDate
                )
            ) {
                reload()
            }
        }
    }

    LaunchedEffect(view, offset) {
        reload()

        changes.collectLatest {
            reload()
        }
    }

    FullPageLayout(
        maxWidth =
            when (viewType) {
                ScheduleViewType.Schedule -> 800.px
                else -> null
            }
    ) {
        Div({
            style {
                position(Position.Relative)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)

                when (viewType) {
                    ScheduleViewType.Schedule -> {
                        padding(1.5.r, 1.r, 0.r, 1.r)
                    }

                    ScheduleViewType.Calendar -> {
                        padding(1.r)
                        property("height", "calc(${100.percent} - ${2.r})")
                    }
                }
            }
        }) {
            when (viewType) {
                ScheduleViewType.Schedule -> {
                    var today = offset

                    IconButton(
                        "keyboard_arrow_up", when (view) {
                            ScheduleView.Daily -> appString { previousDay }
                            ScheduleView.Weekly -> appString { previousWeek }
                            ScheduleView.Monthly -> appString { previousMonth }
                            ScheduleView.Yearly -> appString { previousYear }
                        }) {
                        move(-1.0)
                    }

                    (0 until range[view]!!).forEach { index ->
                        val start = when (view) {
                            ScheduleView.Daily -> startOfDay(today)
                            ScheduleView.Weekly -> startOfWeek(today)
                            ScheduleView.Monthly -> startOfMonth(today)
                            ScheduleView.Yearly -> startOfYear(today)
                        }

                        val end = when (view) {
                            ScheduleView.Daily -> addDays(start, 1.0)
                            ScheduleView.Weekly -> addWeeks(start, 1.0)
                            ScheduleView.Monthly -> addMonths(start, 1.0)
                            ScheduleView.Yearly -> addYears(start, 1.0)
                        }

                        Period(
                            view = view,
                            start = start,
                            end = end,
                            events = if (isLoading) null else shownEvents.filter { event ->
                                (isAfter(event.date, start) || isEqual(event.date, start)) && isBefore(event.date, end)
                            },
                            onUpdate = {
                                scope.launch {
                                    changes.emit(Unit)
                                    onUpdate(it.reminder)
                                }
                            },
                            onOpen = {
                                onReminder(it.reminder)
                            }
                        )

                        today = end
                    }

                    IconButton(
                        "keyboard_arrow_down", when (view) {
                            ScheduleView.Daily -> appString { nextDay }
                            ScheduleView.Weekly -> appString { nextWeek }
                            ScheduleView.Monthly -> appString { nextMonth }
                            ScheduleView.Yearly -> appString { nextYear }
                        }) {
                        move(1.0)
                    }
                }

                ScheduleViewType.Calendar -> {
                    var today = offset
                    val columnInfos = (0 until range[view]!!).map { index ->
                        val start = when (view) {
                            ScheduleView.Daily -> startOfDay(today)
                            ScheduleView.Weekly -> startOfWeek(today)
                            ScheduleView.Monthly -> startOfMonth(today)
                            ScheduleView.Yearly -> startOfYear(today)
                        }

                        val end = when (view) {
                            ScheduleView.Daily -> addDays(start, 1.0)
                            ScheduleView.Weekly -> addWeeks(start, 1.0)
                            ScheduleView.Monthly -> addMonths(start, 1.0)
                            ScheduleView.Yearly -> addYears(start, 1.0)
                        }

                        val result = if (isLoading) emptyList() else shownEvents.filter { event ->
                            (isAfter(event.date, start) || isEqual(event.date, start)) && isBefore(
                                event.date,
                                end
                            )
                        }
                        today = end

                        ColumnInfo(start, end, result)
                    }

                    val millisecondsIn1Rem = remember(view) {
                        when (view) {
                            ScheduleView.Daily -> 15.minutes
                            ScheduleView.Weekly -> 1.hours
                            ScheduleView.Monthly -> 1.hours
                            ScheduleView.Yearly -> 1.hours
                        }.inWholeMilliseconds
                    }

                    var dropTo by remember {
                        mutableStateOf<Long?>(null)
                    }

                    // Events

                    FlexColumns(
                        columnCount = range[view]!!,
                        padding = .125f.r,
                        style = {
                            width(100.percent)
                            height(100.percent)
                        },
                        columnAttrs = { index ->
                            style {
                                height(
                                    ((columnInfos[index].end.getTime() - columnInfos[index].start.getTime()) / millisecondsIn1Rem).r
                                )
                            }

                            // Required for onDrop to work
                            onDragOver {
                                it.preventDefault()
                                dropTo = it.dragToDate(columnInfos[index], millisecondsIn1Rem)
                            }

                            onDragLeave {
                                dropTo = null
                            }

                            onDragEnd {
                                dropTo = null
                            }

                            onDrop {
                                it.preventDefault()

                                dropTo = null

                                val toDate = it.dragToDate(columnInfos[index], millisecondsIn1Rem) ?: return@onDrop
                                moveOccurrence(
                                    dragData = json.decodeFromString<ReminderDragData>(
                                        it.dataTransfer!!.getData(ContentType.Application.Json.toString())
                                    ),
                                    toDate = Instant.fromEpochMilliseconds(toDate)
                                )
                            }

                            onClick {
                                val toDate = it.dragToDate(columnInfos[index], millisecondsIn1Rem) ?: return@onClick
                                val start = Instant.fromEpochMilliseconds(toDate)

                                dropTo = toDate

                                scope.launch {
                                    inputDialog(
                                        title = start.toJSDate().format(),
                                        confirmButton = application.appString { createReminder }
                                    )?.notBlank?.let { reminderTitle ->
                                        val timezone = systemTimezone

                                        api.newReminder(
                                            reminder = Reminder(
                                                title = reminderTitle,
                                                start = start,
                                                timezone = timezone,
                                                utcOffset = rawTimeZones.toList()
                                                    .firstOrNull { it.name == timezone }?.rawOffsetInMinutes?.toDouble()
                                                    ?.div(60.0) ?: 0.0,
                                            )
                                        ) {
                                            reload()
                                        }
                                    }

                                    dropTo = null
                                }
                            }
                        }
                    ) { index ->
                        val columnInfo = columnInfos[index]
                        val columnDurationMs = (columnInfos[index].end.getTime() - columnInfos[index].start.getTime())

                        // Lines

                        val numberOfLines = remember(view) {
                            when (view) {
                                ScheduleView.Daily -> 24
                                ScheduleView.Weekly -> 24 * 7 / 6
                                ScheduleView.Monthly -> ceil(columnDurationMs / 1000 / 60 / 60 / 24).toInt() * 2
                                ScheduleView.Yearly -> ceil(columnDurationMs / 1000 / 60 / 60 / 24).toInt()
                            }
                        }
                        val interval = columnDurationMs / numberOfLines
                        val intervalInRem = interval / millisecondsIn1Rem

                        (0 until numberOfLines).forEach { lineIndex ->
                            Div({
                                classes(Styles.calendarLine)

                                style {
                                    top((lineIndex * intervalInRem).r)
                                }
                            }) {
                                Span({
                                    style {
                                        opacity(.5)
                                        fontSize(12.px)
                                    }
                                }) {
                                    val text = addMilliseconds(
                                        columnInfos[index].start,
                                        interval * lineIndex
                                    ).formatSecondary(view)
                                    Span({
                                        title(text)
                                    }) {
                                        Text(text)
                                    }
                                }
                            }
                        }

                        // Now line

                        val now = Date()

                        if (isAfter(now, columnInfo.start) && isBefore(now, columnInfo.end)) {
                            var tickTock by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    // Delay until start of next minute
                                    val now = Date()
                                    delay(
                                        (startOfMinute(addMinutes(now, 1.0)).getTime() - now.getTime()).milliseconds + 1.seconds
                                    )
                                    tickTock = !tickTock

                                    // Auto-advance next day
                                    if (startOfDay(now) != startOfDay(Date())) {
                                        offset = startOfDay(Date())
                                    }
                                }
                            }
                            key(tickTock) {
                                Div({
                                    classes(Styles.calendarLine, Styles.calendarLineNow)

                                    style {
                                        height(3.px)
                                        borderRadius(1.r)
                                        top(((now.getTime() - columnInfo.start.getTime()) / millisecondsIn1Rem).r)
                                    }

                                    title(
                                        bulletedString(
                                            application.appString { this.now },
                                            now.format()
                                        )
                                    )
                                })
                            }
                        }

                        // Drop line

                        dropTo?.let { dropTo ->
                            if (isAfter(Date(dropTo), columnInfo.start) && isBefore(Date(dropTo), columnInfo.end)) {
                                Div({
                                    classes(Styles.calendarLine, Styles.calendarLineDrop)

                                    style {
                                        height(2.px)
                                        opacity(.5f)
                                        borderRadius(1.r)
                                        top(((dropTo - columnInfo.start.getTime()) / millisecondsIn1Rem).r)
                                    }
                                })
                            }
                        }

                        // Events

                        columnInfo.events.forEach { event ->
                            Div({
                                style {
                                    position(Position.Absolute)
                                    left(0.r)
                                    right(1.r)
                                    top(
                                        ((event.date.getTime() - columnInfos[index].start.getTime()) / millisecondsIn1Rem).r
                                    )
                                }
                            }) {
                                CalendarEvent(
                                    event = event,
                                    view = view,
                                    millisecondsIn1Rem = millisecondsIn1Rem,
                                    onUpdate = {
                                        scope.launch {
                                            changes.emit(Unit)
                                            onUpdate(event.reminder)
                                        }
                                    },
                                    onOpen = { onReminder(event.reminder) }
                                )
                            }
                        }
                    }

                    // Column title

                    FlexColumns(
                        columnCount = range[view]!!,
                        padding = .125f.r,
                        style = {
                            position(Position.Absolute)
                            top(1.r)
                            left(1.r)
                            right(1.r)
                            property("pointer-events", "none")
                        }
                    ) { index ->
                        val text = columnInfos[index].start.formatTitle(view)

                        var hideTitle by remember(view) { mutableStateOf(false) }

                        LaunchedEffect(hideTitle) {
                            if (hideTitle) {
                                delay(5.seconds)
                                hideTitle = false
                            }
                        }

                        Div({
                            classes(Styles.calendarColumnTitle)

                            if (hideTitle) {
                                classes(Styles.calendarColumnTitleHidden)
                            }

                            title(text)

                            style {
                                if (!hideTitle) {
                                    property("pointer-events", "all")
                                    cursor("pointer")
                                }
                            }

                            onClick {
                                hideTitle = true
                            }
                        }) {
                            Text(text)
                        }
                    }

                    // Navigation

                    IconButton(
                        name = "keyboard_arrow_left",
                        background = true,
                        title = when (view) {
                            ScheduleView.Daily -> appString { previousDay }
                            ScheduleView.Weekly -> appString { previousWeek }
                            ScheduleView.Monthly -> appString { previousMonth }
                            ScheduleView.Yearly -> appString { previousYear }
                        },
                        styles = {
                            position(Position.Absolute)
                            top(50.percent)
                            left(1.5.r)
                            transform {
                                translateY(-50.percent)
                            }
                            opacity(.8f)
                            shadow()
                        }
                    ) {
                        move(-1.0)
                    }

                    IconButton(
                        name = "keyboard_arrow_right",
                        background = true,
                        title = when (view) {
                            ScheduleView.Daily -> appString { nextDay }
                            ScheduleView.Weekly -> appString { nextWeek }
                            ScheduleView.Monthly -> appString { nextMonth }
                            ScheduleView.Yearly -> appString { nextYear }
                        },
                        styles = {
                            position(Position.Absolute)
                            top(50.percent)
                            right(1.5.r)
                            transform {
                                translateY(-50.percent)
                            }
                            opacity(.8f)
                            shadow()
                        }
                    ) {
                        move(1.0)
                    }
                }
            }
        }
    }
}

private fun SyntheticMouseEvent.dragToDate(columnInfo: ColumnInfo, millisecondsIn1Rem: Long): Long? {
    val dropTarget = (currentTarget ?: target) as? HTMLElement ?: return null
    val boundingRect = dropTarget.getBoundingClientRect()
    val y = clientY - boundingRect.top

    return (columnInfo.start.getTime() + y.toRem() * millisecondsIn1Rem).toLong().quantize(15 * 60 * 1000)
}
