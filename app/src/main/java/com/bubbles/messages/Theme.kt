package com.bubbles.messages

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF0A84FF)
val Gray = Color(0xFF8E8E93)

@Composable
fun MessagesTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme())
        darkColorScheme(
            primary = Blue, background = Color.Black, surface = Color.Black,
            onBackground = Color.White, onSurface = Color.White
        )
    else
        lightColorScheme(
            primary = Blue, background = Color.White, surface = Color.White,
            onBackground = Color.Black, onSurface = Color.Black
        )
    MaterialTheme(colorScheme = scheme, content = content)
}
