package ru.plumsoftware.focusstudio.ui.screen.editor.photo.data

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorMatrix

data class PhotoSettings(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val hue: Float = 0f,
    val blur: Float = 0f,
    val skewX: Float = 0f,
    val selectedFilter: ColorMatrix? = null,
    val filterName: String = "None",
    val aspectRatio: Float? = null,
    val cropRect: Rect = Rect(0f, 0f, 1f, 1f), // Область кадрирования
    val texts: List<TextElement> = emptyList(),
    val shapes: List<ShapeElement> = emptyList()
)