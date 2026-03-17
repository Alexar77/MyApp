package com.example.habittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.theme.MyAppTheme
import com.example.habittracker.ui.viewmodel.StatsUiState
import com.example.habittracker.ui.viewmodel.StatsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${uiState.habitName} insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        ScreenBackground {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumCard(accent = MyAppTheme.extraColors.success) {
                    PillLabel(
                        text = "Habit rhythm",
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.currentStreak} day streak",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = if (uiState.currentStreak > 0) {
                            "You have active momentum. Protect it with one clean repeat today."
                        } else {
                            "Restart with one completed day. The curve changes quickly after the first repeat."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Longest streak",
                        value = uiState.longestStreak.toString()
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total completions",
                        value = uiState.totalCompletions.toString()
                    )
                }

                SurfaceCard {
                    SectionHeader(
                        title = "Recent consistency",
                        subtitle = "Last 14 business days"
                    )
                    ConsistencyStrip(uiState)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    SurfaceCard(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.displayMedium)
    }
}

@Composable
private fun ConsistencyStrip(uiState: StatsUiState) {
    val formatter = DateTimeFormatter.ISO_DATE
    val days = (13 downTo 0).map { uiState.businessToday.minusDays(it.toLong()) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            val isDone = uiState.recentCompletedDates.contains(day.format(formatter))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isDone) 64.dp else 34.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isDone) MyAppTheme.extraColors.success.copy(alpha = 0.88f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Text(
                    text = day.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MyAppTheme.extraColors.success,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Filled bars show completed days in the last two weeks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
