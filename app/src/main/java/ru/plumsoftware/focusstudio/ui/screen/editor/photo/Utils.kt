package ru.plumsoftware.focusstudio.ui.screen.editor.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import java.io.OutputStream
import androidx.core.graphics.withSave
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.createBitmap

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

// --- ГЛАВНАЯ ФУНКЦИЯ ЭКСПОРТА ---
fun saveEditedImage(
    context: Context,
    originalUri: Uri,
    settings: PhotoSettings,
    displaySize: IntSize,
    onComplete: (Uri?) -> Unit
) {
    val resolver = context.contentResolver
    val inputStream = resolver.openInputStream(originalUri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return

    // 1. Считаем коэффициент масштабирования (Экран -> Оригинал)
    val scaleFactor = originalBitmap.width.toFloat() / displaySize.width.toFloat()

    // 2. Параметры кропа
    val crop = settings.cropRect
    val left = (originalBitmap.width * crop.left).toInt()
    val top = (originalBitmap.height * crop.top).toInt()
    val width = (originalBitmap.width * (crop.right - crop.left)).toInt()
    val height = (originalBitmap.height * (crop.bottom - crop.top)).toInt()

    // 3. Вырезаем кусок
    val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height)

    // 4. Применяем фильтры и РАЗМЫТИЕ (RenderScript для API 23)
    val processedBitmap = applyEffectsToBitmap(context, croppedBitmap, settings)

    // 5. Создаем финальный холст высокого разрешения
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(processedBitmap, 0f, 0f, null)

    // 6. Отрисовка ФИГУР (теперь они точно попадут на фото)
    settings.shapes.forEach { shape ->
        canvas.withSave {
            // Масштабируем и двигаем фигуру относительно вырезанного куска
            val posX = (shape.position.x * scaleFactor) - left
            val posY = (shape.position.y * scaleFactor) - top

            translate(posX, posY)
            rotate(shape.rotation)

            val shapeW = shape.size.width * scaleFactor
            val shapeH = shape.size.height * scaleFactor
            val path = createAndroidShapePath(shape.type, shapeW, shapeH)

            if (shape.fillColor != androidx.compose.ui.graphics.Color.Transparent) {
                drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = shape.fillColor.toArgb()
                    style = Paint.Style.FILL
                })
            }
            if (shape.strokeColor != androidx.compose.ui.graphics.Color.Transparent) {
                drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = shape.strokeColor.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * scaleFactor
                })
            }
        }
    }

    // 7. Отрисовка ТЕКСТА
    settings.texts.forEach { text ->
        // Используем TextPaint вместо обычного Paint (нужен для StaticLayout)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = text.color.toArgb()
            textSize = text.fontSize * scaleFactor
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val posX = (text.position.x * scaleFactor) - left
        val posY = (text.position.y * scaleFactor) - top

        // Измеряем ширину текста для макета (чтобы он не обрезался)
        // Если в редакторе есть фиксированная ширина, лучше использовать её, помноженную на scaleFactor
        val textWidth = textPaint.measureText(text.text).toInt().coerceAtLeast(1)

        // Создаем макет многострочного текста
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text.text, 0, text.text.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text.text,
                textPaint,
                textWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
            )
        }

        // Рисуем текст через Layout
        canvas.withSave {
            translate(posX, posY) // Перемещаемся в нужную точку
            staticLayout.draw(this) // Рисуем весь блок текста со всеми переносами
        }
    }

    // 8. Сохранение в галерею
    val filename = "Focus_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FocusStudio")
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        resolver.openOutputStream(it)?.use { out ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    // Чистка
    croppedBitmap.recycle()
    processedBitmap.recycle()

    onComplete(uri)
}

// --- ВСПОМОГАТЕЛЬНЫЕ ПУТИ ФИГУР ---
fun createAndroidShapePath(type: ShapeType, w: Float, h: Float): android.graphics.Path {
    val path = android.graphics.Path()
    when (type) {
        ShapeType.SQUARE -> path.addRect(0f, 0f, w, h, android.graphics.Path.Direction.CW)
        ShapeType.CIRCLE -> path.addOval(RectF(0f, 0f, w, h), android.graphics.Path.Direction.CW)
        ShapeType.TRIANGLE -> {
            path.moveTo(w / 2f, 0f); path.lineTo(w, h); path.lineTo(0f, h); path.close()
        }
        ShapeType.STAR -> {
            val cx = w / 2f; val cy = h / 2f
            var angle = Math.PI / 2 * 3; val step = Math.PI / 5
            path.moveTo(cx, cy - w / 2f)
            repeat(5) {
                path.lineTo((cx + cos(angle) * w / 2f).toFloat(), (cy + sin(angle) * w / 2f).toFloat())
                angle += step
                path.lineTo((cx + cos(angle) * w / 4f).toFloat(), (cy + sin(angle) * w / 4f).toFloat())
                angle += step
            }
            path.close()
        }
        ShapeType.ARROW -> {
            path.moveTo(0f, h / 2f); path.lineTo(w, h / 2f)
            path.lineTo(w * 0.7f, h * 0.2f); path.moveTo(w, h / 2f); path.lineTo(w * 0.7f, h * 0.8f)
        }
    }
    return path
}

// --- ФУНКЦИЯ ОБРАБОТКИ ЭФФЕКТОВ (API 23+) ---
fun applyEffectsToBitmap(context: Context, bitmap: Bitmap, settings: PhotoSettings): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    // 1. Цветокоррекция (Яркость, Контраст, Фильтры)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val composeMatrix = getCombinedMatrix(settings)
    paint.colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix(composeMatrix.values))
    canvas.drawBitmap(result, 0f, 0f, paint)

    // 2. РАЗМЫТИЕ через RenderScript (Работает на API 23+)
    if (settings.blur > 0.1f) {
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, result)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        // В RenderScript радиус макс. 25. Если нужно больше — нужно делать в несколько проходов.
        val radius = settings.blur.coerceIn(0.1f, 25f)
        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(result)

        rs.destroy()
    }

    return result
}