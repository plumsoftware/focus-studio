package ru.plumsoftware.focusstudio.ui.screen.editor.photo.data

import androidx.compose.ui.graphics.ColorMatrix

// Цветовые матрицы для фильтров
object FilterMatrices {
    val None = null
    val Vintage = ColorMatrix(
        floatArrayOf(
            0.9f, 0.1f, 0.1f, 0f, 0f,
            0.1f, 0.8f, 0.1f, 0f, 0f,
            0.1f, 0.1f, 0.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val Noir = ColorMatrix().apply { setToSaturation(0f) }
    val Cinema = ColorMatrix(
        floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 1.1f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val Warm = ColorMatrix(
        floatArrayOf(
            1.1f, 0f, 0f, 0f, 10f,
            0f, 1f, 0f, 0f, 5f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val Cold = ColorMatrix(
        floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 5f,
            0f, 0f, 1.2f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val Vivid = ColorMatrix().apply { setToSaturation(1.5f) }
}