package com.example.habittracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.floor

private val CalendarCellCornerRadius = 12.dp
private val CalendarCellGap = 6.dp
private val CalendarWeekdayGap = 8.dp
private val CalendarPadding = 6.dp
private val CalendarIndicatorRadius = 3.dp
private val CalendarIndicatorStroke = 1.5.dp
private val CalendarStatusHeight = 4.dp
private val CalendarStatusWidthFraction = 0.52f

private data class CalendarCellState(
    val date: LocalDate?,
    val dateKey: String?,
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val isMissed: Boolean,
    val hasBirthday: Boolean,
    val hasNote: Boolean
)

private data class CalendarGridState(
    val cells: List<CalendarCellState>,
    val weekCount: Int
)

@Composable
fun MonthCalendar(
    month: YearMonth,
    completedDates: Set<String>,
    scheduledDates: Set<String>,
    birthdayDates: Set<String>,
    noteDates: Set<String>,
    todayDate: LocalDate,
    onToggleDate: (String) -> Unit,
    onOpenDayNote: (String) -> Unit,
    interactive: Boolean = true
) {
    val weekdayLabels = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    val gridState = remember(
        month,
        completedDates,
        scheduledDates,
        birthdayDates,
        noteDates,
        todayDate
    ) {
        buildCalendarGridState(
            month = month,
            completedDates = completedDates,
            scheduledDates = scheduledDates,
            birthdayDates = birthdayDates,
            noteDates = noteDates,
            todayDate = todayDate
        )
    }

    val weekdayLayouts = remember(weekdayLabels, typography, colorScheme, textMeasurer) {
        weekdayLabels.map { label ->
            textMeasurer.measure(
                text = label,
                style = typography.labelMedium.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
    }

    val dayLayouts = remember(gridState.cells, typography, colorScheme, textMeasurer) {
        gridState.cells.map { cell ->
            cell.date?.let { date ->
                textMeasurer.measure(
                    text = date.dayOfMonth.toString(),
                    style = typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (cell.isScheduled) {
                            colorScheme.onSurface
                        } else {
                            colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        }
                    )
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gapPx = with(density) { CalendarCellGap.toPx() }
        val weekdayGapPx = with(density) { CalendarWeekdayGap.toPx() }
        val paddingPx = with(density) { CalendarPadding.toPx() }
        val cornerRadiusPx = with(density) { CalendarCellCornerRadius.toPx() }
        val indicatorRadiusPx = with(density) { CalendarIndicatorRadius.toPx() }
        val indicatorStrokePx = with(density) { CalendarIndicatorStroke.toPx() }
        val statusHeightPx = with(density) { CalendarStatusHeight.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val cellSizePx = (widthPx - gapPx * 6f) / 7f
        val weekdayHeightPx = weekdayLayouts.maxOfOrNull { it.size.height }?.toFloat() ?: 0f
        val gridHeightPx = gridState.weekCount * cellSizePx + (gridState.weekCount - 1) * gapPx
        val totalHeightPx = weekdayHeightPx + weekdayGapPx + gridHeightPx
        val totalHeightDp = with(density) { totalHeightPx.toDp() }

        val pointerModifier = Modifier.pointerInput(
            month,
            gridState,
            interactive,
            onToggleDate,
            onOpenDayNote
        ) {
            detectTapGestures(
                onTap = { offset ->
                    if (!interactive) return@detectTapGestures
                    resolveTappedDate(
                        offset = offset,
                        weekdayHeightPx = weekdayHeightPx,
                        weekdayGapPx = weekdayGapPx,
                        cellSizePx = cellSizePx,
                        gapPx = gapPx,
                        cells = gridState.cells
                    )?.dateKey?.let(onToggleDate)
                },
                onLongPress = { offset ->
                    if (!interactive) return@detectTapGestures
                    val tappedCell = resolveTappedDate(
                        offset = offset,
                        weekdayHeightPx = weekdayHeightPx,
                        weekdayGapPx = weekdayGapPx,
                        cellSizePx = cellSizePx,
                        gapPx = gapPx,
                        cells = gridState.cells
                    )
                    if (tappedCell?.isScheduled == true) {
                        tappedCell.dateKey?.let(onOpenDayNote)
                    }
                }
            )
        }

        Canvas(
            modifier = pointerModifier
                .fillMaxWidth()
                .height(totalHeightDp)
        ) {
            weekdayLayouts.forEachIndexed { index, layout ->
                val x = index * (cellSizePx + gapPx) + (cellSizePx - layout.size.width) / 2f
                val y = (weekdayHeightPx - layout.size.height) / 2f
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x, y)
                )
            }

            val gridStartY = weekdayHeightPx + weekdayGapPx
            val completedBackground = Color(0xFF1B5E20)
            val completedIndicator = Color(0xFF69F0AE)
            val missedBackground = colorScheme.errorContainer
            val scheduledBackground = colorScheme.surfaceVariant
            val disabledBackground = colorScheme.surface.copy(alpha = 0.7f)
            val noteIndicator = colorScheme.tertiary
            val birthdayIndicator = colorScheme.primary
            val missedIndicator = colorScheme.error

            gridState.cells.forEachIndexed { index, cell ->
                val row = index / 7
                val column = index % 7
                val x = column * (cellSizePx + gapPx)
                val y = gridStartY + row * (cellSizePx + gapPx)

                if (cell.date == null) return@forEachIndexed

                val backgroundColor = when {
                    cell.isCompleted -> completedBackground
                    cell.isMissed -> missedBackground
                    cell.isScheduled -> scheduledBackground
                    else -> disabledBackground
                }

                drawRoundRect(
                    color = backgroundColor,
                    topLeft = Offset(x, y),
                    size = Size(cellSizePx, cellSizePx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )

                if (cell.hasBirthday) {
                    drawCircle(
                        color = birthdayIndicator,
                        radius = indicatorRadiusPx + indicatorStrokePx,
                        center = Offset(x + paddingPx + indicatorRadiusPx, y + paddingPx + indicatorRadiusPx),
                        style = Stroke(width = indicatorStrokePx)
                    )
                }

                if (cell.hasNote) {
                    drawCircle(
                        color = noteIndicator,
                        radius = indicatorRadiusPx,
                        center = Offset(x + cellSizePx - paddingPx - indicatorRadiusPx, y + paddingPx + indicatorRadiusPx)
                    )
                }

                val dayLayout = dayLayouts[index] ?: return@forEachIndexed
                drawText(
                    textLayoutResult = dayLayout,
                    topLeft = Offset(
                        x + (cellSizePx - dayLayout.size.width) / 2f,
                        y + (cellSizePx - dayLayout.size.height) / 2f - statusHeightPx
                    )
                )

                if (cell.isCompleted || cell.isMissed) {
                    val statusWidth = cellSizePx * CalendarStatusWidthFraction
                    drawRoundRect(
                        color = if (cell.isCompleted) completedIndicator else missedIndicator,
                        topLeft = Offset(
                            x + (cellSizePx - statusWidth) / 2f,
                            y + cellSizePx - paddingPx - statusHeightPx
                        ),
                        size = Size(statusWidth, statusHeightPx),
                        cornerRadius = CornerRadius(statusHeightPx, statusHeightPx)
                    )
                }
            }
        }
    }
}

private fun buildCalendarGridState(
    month: YearMonth,
    completedDates: Set<String>,
    scheduledDates: Set<String>,
    birthdayDates: Set<String>,
    noteDates: Set<String>,
    todayDate: LocalDate
): CalendarGridState {
    val firstDay = month.atDay(1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val cells = buildList {
        repeat(leadingEmptyCells) { add(null) }
        (1..dayCount).forEach { day -> add(month.atDay(day)) }
        while (size % 7 != 0) add(null)
    }.map { date ->
        val dateKey = date?.toString()
        val isScheduled = dateKey != null && dateKey in scheduledDates
        val isCompleted = isScheduled && dateKey in completedDates
        CalendarCellState(
            date = date,
            dateKey = dateKey,
            isScheduled = isScheduled,
            isCompleted = isCompleted,
            isMissed = date != null && isScheduled && !date.isAfter(todayDate) && !isCompleted,
            hasBirthday = dateKey != null && dateKey in birthdayDates,
            hasNote = dateKey != null && dateKey in noteDates
        )
    }
    return CalendarGridState(
        cells = cells,
        weekCount = cells.size / 7
    )
}

private fun resolveTappedDate(
    offset: Offset,
    weekdayHeightPx: Float,
    weekdayGapPx: Float,
    cellSizePx: Float,
    gapPx: Float,
    cells: List<CalendarCellState>
): CalendarCellState? {
    val gridStartY = weekdayHeightPx + weekdayGapPx
    if (offset.y < gridStartY) return null

    val rowHeight = cellSizePx + gapPx
    val columnWidth = cellSizePx + gapPx
    val row = floor((offset.y - gridStartY) / rowHeight).toInt()
    val column = floor(offset.x / columnWidth).toInt()
    if (row < 0 || column !in 0..6) return null

    val localX = offset.x - column * columnWidth
    val localY = offset.y - gridStartY - row * rowHeight
    if (localX !in 0f..cellSizePx || localY !in 0f..cellSizePx) return null

    val index = row * 7 + column
    if (index !in cells.indices) return null
    return cells[index]
}
