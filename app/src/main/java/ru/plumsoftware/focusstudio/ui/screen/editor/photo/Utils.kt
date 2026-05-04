package ru.plumsoftware.focusstudio.ui.screen.editor.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings

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

fun calculateRectForRatio(ratio: Float?): Rect {
    if (ratio == null) return Rect(0.1f, 0.1f, 0.9f, 0.9f)

    // Вписываем пресет в центр (нормализованные координаты 0..1)
    return if (ratio > 1f) { // Горизонтальный (напр. 16:9)
        val h = 0.8f / ratio
        Rect(0.1f, 0.5f - h/2f, 0.9f, 0.5f + h/2f)
    } else { // Вертикальный (напр. 3:4)
        val w = 0.8f * ratio
        Rect(0.5f - w/2f, 0.1f, 0.5f + w/2f, 0.9f)
    }
}

fun Path.addStar(size: Size, spikes: Int = 5, outerRadius: Float, innerRadius: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    var angle = Math.PI / 2 * 3
    val step = Math.PI / spikes

    moveTo(center.x, (center.y - outerRadius))
    repeat(spikes) {
        lineTo(
            (center.x + Math.cos(angle) * outerRadius).toFloat(),
            (center.y + Math.sin(angle) * outerRadius).toFloat()
        )
        angle += step
        lineTo(
            (center.x + Math.cos(angle) * innerRadius).toFloat(),
            (center.y + Math.sin(angle) * innerRadius).toFloat()
        )
        angle += step
    }
    close()
}

fun Path.addArrow(size: Size) {
    val w = size.width
    val h = size.height
    moveTo(0f, h / 2)
    lineTo(w, h / 2)
    lineTo(w * 0.7f, h * 0.2f)
    moveTo(w, h / 2)
    lineTo(w * 0.7f, h * 0.8f)
}