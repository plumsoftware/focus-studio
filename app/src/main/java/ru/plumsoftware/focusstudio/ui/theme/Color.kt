package ru.plumsoftware.focusstudio.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val iOSBlue = Color(0xFF007AFF)
val iOSPurple = Color(0xFFA352FC)
val DarkBg = Color(0xFF000000)
val DarkSurface = Color(0xFF151517)
val LightBg = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val AppleGray = Color(0xFF8E8E93)
val AccentBlue = Color(0xFF3A7AFE)
val AccentGreen = Color(0xFF2DC97E)

val GradientAccent = Brush.linearGradient(
    colors = listOf(Color(0xFF6C5CE7), Color(0xFFFF5A8A))
)
val AccentStart = Color(0xFF6C5CE7) // для бордеров/иконок, где градиент неудобен
val AccentEnd = Color(0xFFFF5A8A)