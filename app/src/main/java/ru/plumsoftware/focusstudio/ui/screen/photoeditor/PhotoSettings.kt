package ru.plumsoftware.focusstudio.ui.screen.photoeditor

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter

data class PhotoSettings(
    val brightness: Float = 0f,    // -100..100
    val contrast: Float = 0f,      // -100..100 (теперь сырое)
    val saturation: Float = 1f,    // 0..2
    val skewX: Float = 0f,
    val selectedFilter: ColorMatrix? = null,
    val filterName: String = "None",
    val aspectRatio: Float? = null, // null = свободная форма
    val isCropMode: Boolean = false
)

// Цветовые матрицы для фильтров
object FilterMatrices {
    val None = null
    val Vintage = ColorMatrix(floatArrayOf(
        0.9f, 0.1f, 0.1f, 0f, 0f,
        0.1f, 0.8f, 0.1f, 0f, 0f,
        0.1f, 0.1f, 0.5f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    // У Compose ColorMatrix есть метод setToSaturation
    val Noir = ColorMatrix().apply { setToSaturation(0f) }
    val Cinema = ColorMatrix(floatArrayOf(
        0.8f, 0f, 0f, 0f, 0f,
        0f, 1.1f, 0f, 0f, 0f,
        0f, 0f, 1.2f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
}
