package com.example.habittracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import java.util.Locale

data class TrendPoint(
    val label: String,
    val value: Double
)

data class TrendSeries(
    val points: List<TrendPoint>,
    val color: Color
)

@Composable
fun TrendChart(
    points: List<TrendPoint>,
    secondaryPoints: List<TrendPoint> = emptyList(),
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty() && secondaryPoints.isEmpty()) return

    val colorScheme = MaterialTheme.colorScheme
    val primarySeries = TrendSeries(points = points, color = colorScheme.primary)
    val secondarySeries = secondaryPoints.takeIf { it.isNotEmpty() }?.let {
        TrendSeries(points = it, color = secondaryColor)
    }
    val allSeries = listOfNotNull(primarySeries.takeIf { it.points.isNotEmpty() }, secondarySeries)
    val chartHeight = 220.dp
    val minValue = allSeries.minOf { series -> series.points.minOf { it.value } }
    val maxValue = allSeries.maxOf { series -> series.points.maxOf { it.value } }
    val tickCount = 4
    val yLabels = remember(allSeries) { buildYLabels(minValue, maxValue, tickCount) }
    val xLabels = remember(points, secondaryPoints) { buildXLabels(points.ifEmpty { secondaryPoints }) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yLabels.reversed().forEach { label ->
                    Text(
                        text = formatAxisValue(label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                ) {
                    val padding = 12.dp.toPx()
                    val chartWidthPx = size.width - padding * 2
                    val chartHeightPx = size.height - padding * 2
                    if (chartWidthPx <= 0f || chartHeightPx <= 0f) return@Canvas

                    val minValuePx = minValue.toFloat()
                    val maxValuePx = maxValue.toFloat()
                    val valueRange = (maxValuePx - minValuePx).takeIf { it > 0f } ?: 1f
                    yLabels.forEach { label ->
                        val normalizedY = ((label.toFloat() - minValuePx) / valueRange).coerceIn(0f, 1f)
                        val y = padding + chartHeightPx - (normalizedY * chartHeightPx)
                        drawLine(
                            color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                            start = Offset(padding, y),
                            end = Offset(size.width - padding, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    drawLine(
                        color = colorScheme.outlineVariant,
                        start = Offset(padding, size.height - padding),
                        end = Offset(size.width - padding, size.height - padding),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawLine(
                        color = colorScheme.outlineVariant,
                        start = Offset(padding, padding),
                        end = Offset(padding, size.height - padding),
                        strokeWidth = 2.dp.toPx()
                    )

                    allSeries.forEach { series ->
                        val xStep = if (series.points.size == 1) 0f else chartWidthPx / series.points.lastIndex
                        val offsets = series.points.mapIndexed { index, point ->
                            val normalizedY = (point.value.toFloat() - minValuePx) / valueRange
                            Offset(
                                x = padding + xStep * index,
                                y = padding + chartHeightPx - (normalizedY * chartHeightPx)
                            )
                        }
                        if (offsets.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(offsets.first().x, offsets.first().y)
                                offsets.drop(1).forEach { offset -> lineTo(offset.x, offset.y) }
                            }
                            drawPath(
                                path = path,
                                color = series.color,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            offsets.forEach { offset ->
                                drawCircle(color = series.color, radius = 4.dp.toPx(), center = offset)
                                drawCircle(color = colorScheme.surface, radius = 2.dp.toPx(), center = offset)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            xLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun buildYLabels(minValue: Double, maxValue: Double, tickCount: Int): List<Double> {
    if (tickCount <= 1) return listOf(minValue, maxValue).distinct()
    if (minValue == maxValue) {
        val base = if (minValue == 0.0) 1.0 else kotlin.math.abs(minValue) * 0.1
        return List(tickCount) { index -> minValue - base + ((2 * base) / (tickCount - 1)) * index }
    }
    val step = (maxValue - minValue) / (tickCount - 1)
    return List(tickCount) { index -> minValue + step * index }
}

private fun buildXLabels(points: List<TrendPoint>, maxLabels: Int = 4): List<String> {
    if (points.isEmpty()) return emptyList()
    if (points.size <= maxLabels) return points.map { it.label }
    val indices = List(maxLabels) { index ->
        ((points.lastIndex.toFloat() / (maxLabels - 1)) * index).toInt()
    }.distinct()
    return indices.map { points[it].label }
}

private fun formatAxisValue(value: Double): String {
    val absolute = kotlin.math.abs(value)
    return when {
        absolute >= 1000 -> String.format(Locale.getDefault(), "%.1fk", value / 1000.0)
        absolute >= 100 -> String.format(Locale.getDefault(), "%.0f", value)
        absolute >= 10 -> String.format(Locale.getDefault(), "%.1f", value)
        else -> String.format(Locale.getDefault(), "%.2f", value)
    }
}
