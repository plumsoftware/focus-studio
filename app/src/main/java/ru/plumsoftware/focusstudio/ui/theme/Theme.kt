package ru.plumsoftware.focusstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun FocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = iOSBlue,
            background = DarkBg,
            surface = DarkSurface,
            onBackground = Color.White,
            onSurface = Color.White,
            secondary = iOSPurple
        )
    } else {
        lightColorScheme(
            primary = iOSBlue,
            background = LightBg,
            surface = LightSurface,
            onBackground = Color.Black,
            onSurface = Color.Black,
            secondary = iOSPurple
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusTypography,
        shapes = FocusShapes,
        content = content
    )
}