package com.example.habittracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val appDarkColorScheme = darkColorScheme()

@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = appDarkColorScheme,
        typography = Typography,
        content = content
    )
}
