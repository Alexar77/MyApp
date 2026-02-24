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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthCalendar(
    month: YearMonth,
    completedDates: Set<String>,
    scheduledDates: Set<String>,
    noteDates: Set<String>,
    createdDate: LocalDate?,
    todayDate: LocalDate,
    onToggleDate: (String) -> Unit,
    onOpenDayNote: (String) -> Unit
) {
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val firstDay = month.atDay(1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()

    val cells = buildList {
        repeat(leadingEmptyCells) { add(null) }
        (1..dayCount).forEach { day -> add(month.atDay(day)) }
        while (size % 7 != 0) add(null)
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

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { date ->
                    val isScheduled = date?.toString() in scheduledDates
                    val isCompleted = isScheduled && date?.toString() in completedDates
                    val isWithinTrackRange = date != null && isScheduled
                    val isMissed = date != null &&
                        isScheduled &&
                        !date.isAfter(todayDate) &&
                        !isCompleted

                    CalendarDayCell(
                        date = date,
                        completed = isCompleted,
                        missed = isMissed,
                        enabled = isWithinTrackRange,
                        hasNote = date?.toString() in noteDates,
                        onToggleDate = onToggleDate,
                        onOpenDayNote = onOpenDayNote,
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
    hasNote: Boolean,
    onToggleDate: (String) -> Unit,
    onOpenDayNote: (String) -> Unit,
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

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .combinedClickable(
                enabled = enabled,
                onClick = { onToggleDate(date.toString()) },
                onLongClick = { onOpenDayNote(date.toString()) }
            )
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
