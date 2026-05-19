package ru.plumsoftware.focusstudio.ui.screen.editor.photo.data

import androidx.compose.ui.graphics.ColorMatrix

// Цветовые матрицы для фильтров
object FilterMatrices {
    val None = null

    // Классическая сепия (эффект старой фотографии)
    val Sepia = ColorMatrix(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Эффект камеры Polaroid
    val Polaroid = ColorMatrix(
        floatArrayOf(
            1.438f, -0.062f, -0.062f, 0f, 0f,
            -0.122f, 1.378f, -0.122f, 0f, 0f,
            -0.016f, -0.016f, 1.483f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Техниколор (яркие красные и зеленые тона, как в старом кино)
    val Technicolor = ColorMatrix(
        floatArrayOf(
            1.912f, -0.854f, -0.091f, 0f, 11.7f,
            -0.308f, 1.765f, -0.106f, 0f, -14.7f,
            -0.231f, -0.750f, 1.847f, 0f, -12.1f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Драматичный черно-белый (высокий контраст)
    val DramaticBW = ColorMatrix(
        floatArrayOf(
            1.5f, 1.5f, 1.5f, 0f, -128f,
            1.5f, 1.5f, 1.5f, 0f, -128f,
            1.5f, 1.5f, 1.5f, 0f, -128f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Эффект прибора ночного видения
    val NightVision = ColorMatrix(
        floatArrayOf(
            0.1f, 0.4f, 0f, 0f, 0f,
            0.3f, 1f, 0.3f, 0f, 0f,
            0f, 0.4f, 0.1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Инверсия цветов (негатив)
    val Invert = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Kodachrome (винтажная пленка с теплыми тенями)
    val Kodachrome = ColorMatrix(
        floatArrayOf(
            1.128f, -0.396f, -0.039f, 0f, 63.7f,
            -0.165f, 1.476f, -0.182f, 0f, 38.5f,
            -0.132f, -0.532f, 1.626f, 0f, 41.6f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    // Старые фильтры для комплекта
    val Vintage = ColorMatrix(floatArrayOf(
        0.9f, 0.1f, 0.1f, 0f, 0f,
        0.1f, 0.8f, 0.1f, 0f, 0f,
        0.1f, 0.1f, 0.5f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    val Noir = ColorMatrix().apply { setToSaturation(0f) }
    val Vivid = ColorMatrix().apply { setToSaturation(1.6f) }
    val Cold = ColorMatrix(floatArrayOf(
        0.8f, 0f, 0f, 0f, 0f,
        0f, 0.9f, 0f, 0f, 10f,
        0f, 0f, 1.3f, 0f, 20f,
        0f, 0f, 0f, 1f, 0f
    ))
}