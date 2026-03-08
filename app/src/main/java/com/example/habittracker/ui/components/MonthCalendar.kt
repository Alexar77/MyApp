package com.example.habittracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

private val CalendarCellShape = RoundedCornerShape(10.dp)

private data class CalendarCellState(
    val date: LocalDate?,
    val dateKey: String?,
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val isMissed: Boolean,
    val hasBirthday: Boolean,
    val hasNote: Boolean
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
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val firstDay = month.atDay(1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()

    val cells = remember(month) {
        buildList {
            repeat(leadingEmptyCells) { add(null) }
            (1..dayCount).forEach { day -> add(month.atDay(day)) }
            while (size % 7 != 0) add(null)
        }
    }
    val cellStates = remember(month, completedDates, scheduledDates, birthdayDates, noteDates, todayDate) {
        cells.map { date ->
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
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        cellStates.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { cellState ->
                    CalendarDayCell(
                        date = cellState.date,
                        completed = cellState.isCompleted,
                        missed = cellState.isMissed,
                        enabled = cellState.isScheduled,
                        hasBirthday = cellState.hasBirthday,
                        hasNote = cellState.hasNote,
                        onToggleDate = onToggleDate,
                        onOpenDayNote = onOpenDayNote,
                        interactive = interactive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    completed: Boolean,
    missed: Boolean,
    enabled: Boolean,
    hasBirthday: Boolean,
    hasNote: Boolean,
    onToggleDate: (String) -> Unit,
    onOpenDayNote: (String) -> Unit,
    interactive: Boolean,
    modifier: Modifier = Modifier
) {
    if (date == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val completedBackground = Color(0xFF1B5E20)
    val completedMarker = Color(0xFF69F0AE)

    val bgColor = when {
        completed -> completedBackground
        missed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val dayColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    val markerText = when {
        completed -> "✓"
        missed -> "✕"
        else -> ""
    }
    val markerColor = when {
        completed -> completedMarker
        missed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val dayModifier = if (interactive) {
        Modifier.combinedClickable(
            enabled = true,
            onClick = { onToggleDate(date.toString()) },
            onLongClick = {
                if (enabled) onOpenDayNote(date.toString())
            }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CalendarCellShape)
            .background(bgColor)
            .then(dayModifier)
            .padding(6.dp)
    ) {
        if (hasNote) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(3.dp)
            )
        }

        if (hasBirthday) {
            Text(
                text = "🎂",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-14).dp, y = (-11).dp),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = dayColor
            )
            Text(
                text = markerText,
                style = MaterialTheme.typography.labelLarge,
                color = markerColor
            )
        }
    }
}
