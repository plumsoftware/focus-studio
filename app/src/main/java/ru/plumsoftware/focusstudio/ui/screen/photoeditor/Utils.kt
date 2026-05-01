package ru.plumsoftware.focusstudio.ui.screen.photoeditor

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter

// 1. Расширение для перемножения (конкатенации) матриц 4x5, так как в Compose нет timesAssign
fun ColorMatrix.concat(second: ColorMatrix) {
    val m1 = this.values
    val m2 = second.values
    val result = FloatArray(20)

    for (row in 0 until 4) {
        for (col in 0 until 4) {
            result[row * 5 + col] =
                m1[row * 5 + 0] * m2[0 * 5 + col] +
                        m1[row * 5 + 1] * m2[1 * 5 + col] +
                        m1[row * 5 + 2] * m2[2 * 5 + col] +
                        m1[row * 5 + 3] * m2[3 * 5 + col]
        }
        // Обработка смещения (5-я колонка)
        result[row * 5 + 4] =
            m1[row * 5 + 0] * m2[0 * 5 + 4] +
                    m1[row * 5 + 1] * m2[1 * 5 + 4] +
                    m1[row * 5 + 2] * m2[2 * 5 + 4] +
                    m1[row * 5 + 3] * m2[3 * 5 + 4] +
                    m1[row * 5 + 4]
    }
    for (i in 0 until 20) m1[i] = result[i]
}

fun getCombinedMatrix(settings: PhotoSettings): ColorMatrix {
    val result = ColorMatrix()

    settings.selectedFilter?.let { result.concat(it) }

    val satMatrix = ColorMatrix().apply { setToSaturation(settings.saturation) }
    result.concat(satMatrix)

    // Контраст: мапим -100..100 в коэффициент 0.5..1.5
    val contrastScale = 1f + (settings.contrast / 200f)
    val b = settings.brightness

    val contrastMatrix = ColorMatrix(floatArrayOf(
        contrastScale, 0f, 0f, 0f, b,
        0f, contrastScale, 0f, 0f, b,
        0f, 0f, contrastScale, 0f, b,
        0f, 0f, 0f, 1f, 0f
    ))
    result.concat(contrastMatrix)

    return result
}
