package com.example.habittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.habittracker.ui.components.PillLabel
import com.example.habittracker.ui.components.PremiumCard
import com.example.habittracker.ui.components.ScreenBackground
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.SurfaceCard
import com.example.habittracker.ui.theme.MyAppTheme

private data class MotivationThemeCard(
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationScreen(onBack: () -> Unit = {}) {
    val themes = listOf(
        MotivationThemeCard("Focus", "Protect your energy and finish one meaningful thing."),
        MotivationThemeCard("Resilience", "Small disciplined days beat occasional perfect days."),
        MotivationThemeCard("Gratitude", "Notice what is steady, not only what is missing.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Motivation") },
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
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MyAppTheme.extraColors.accent,
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        MyAppTheme.extraColors.brandSurface,
                                        MyAppTheme.extraColors.accent.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillLabel(
                                text = "Daily spark",
                                color = MyAppTheme.extraColors.accent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Build the version of yourself you trust on ordinary days.",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "Momentum comes from repeating a calm standard, not waiting for a perfect mood.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SectionHeader(
                    title = "Themes",
                    subtitle = "Swipe across the mindset you need today."
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    themes.forEach { item ->
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MyAppTheme.extraColors.warning)
                            Text(item.title, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SurfaceCard {
                    SectionHeader(title = "Quick ritual", subtitle = "A 60-second reset for low-energy moments.")
                    Text(
                        "1. Breathe in for 4.\n2. Hold for 4.\n3. Exhale for 6.\n4. Write one sentence: What matters next?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                SurfaceCard {
                    SectionHeader(title = "Saved reminders", subtitle = "Short prompts worth revisiting.")
                    Text(
                        "Protect your attention.\nProgress likes consistency.\nMake the next action obvious.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08111F)
@Composable
private fun MotivationScreenPreview() {
    MyAppTheme {
        MotivationScreen()
    }
}
