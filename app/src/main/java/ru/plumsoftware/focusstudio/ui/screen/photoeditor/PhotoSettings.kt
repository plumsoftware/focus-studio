package ru.plumsoftware.focusstudio.ui.screen.photoeditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import java.util.UUID

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
    val texts: List<TextElement> = emptyList()
)

data class TextElement(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Tap to edit",
    val position: Offset = Offset(200f, 200f),
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val fontSize: Float = 24f,
    val color: Color = Color.White,
    val textAlign: TextAlign = TextAlign.Center,
    val fontFamily: String = "sf_pro_regular"
)

object EditorTools {
    const val ADJUST = "adjust"
    const val FILTERS = "filters"
    const val CROP = "crop"
    const val TEXT = "text"
}

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

    val Warm = ColorMatrix(floatArrayOf(
        1.1f, 0f, 0f, 0f, 10f,
        0f, 1f, 0f, 0f, 5f,
        0f, 0f, 0.9f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    val Cold = ColorMatrix(floatArrayOf(
        0.9f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 5f,
        0f, 0f, 1.2f, 0f, 10f,
        0f, 0f, 0f, 1f, 0f
    ))
    val Vivid = ColorMatrix().apply { setToSaturation(1.5f) }
}
