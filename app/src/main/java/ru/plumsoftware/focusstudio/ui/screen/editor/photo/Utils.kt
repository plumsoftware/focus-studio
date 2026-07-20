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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.OutputStream
import androidx.core.graphics.withSave
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.createBitmap
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextBackgroundStyle
import ru.plumsoftware.focusstudio.ui.theme.AccentEnd
import ru.plumsoftware.focusstudio.ui.theme.AccentStart

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

    // Получаем плотность экрана (например, 2.0, 3.0 и т.д.)
    val screenDensity = context.resources.displayMetrics.density

    // --- ШАГ 1: Считаем реальный размер и смещение фото на экране ---
    val bitmapWidth = originalBitmap.width.toFloat()
    val bitmapHeight = originalBitmap.height.toFloat()
    val containerWidth = displaySize.width.toFloat()
    val containerHeight = displaySize.height.toFloat()

    val scaleFit = minOf(containerWidth / bitmapWidth, containerHeight / bitmapHeight)
    val actualImageOnScreenWidth = bitmapWidth * scaleFit
    val actualImageOnScreenHeight = bitmapHeight * scaleFit

    val offsetX = (containerWidth - actualImageOnScreenWidth) / 2f
    val offsetY = (containerHeight - actualImageOnScreenHeight) / 2f

    // Коэффициент масштабирования (Экранные пиксели -> Пиксели файла)
    val scaleFactor = bitmapWidth / actualImageOnScreenWidth

    // --- ШАГ 2: Кроп ---
    val crop = settings.cropRect
    val left = (bitmapWidth * crop.left).toInt()
    val top = (bitmapHeight * crop.top).toInt()
    val width = (bitmapWidth * (crop.right - crop.left)).toInt()
    val height = (bitmapHeight * (crop.bottom - crop.top)).toInt()

    val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height)
    val processedBitmap = applyEffectsToBitmap(context, croppedBitmap, settings)

    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(processedBitmap, 0f, 0f, null)

    // --- ШАГ 3: Отрисовка ФИГУР ---
    settings.shapes.forEach { shape ->
        canvas.withSave {
            val posX = ((shape.position.x - offsetX) * scaleFactor) - left
            val posY = ((shape.position.y - offsetY) * scaleFactor) - top

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
                    // Толщину обводки тоже нужно масштабировать с учетом плотности
                    strokeWidth = 2f * screenDensity * scaleFactor
                })
            }
        }
    }

    // --- ШАГ 4: Отрисовка ТЕКСТА ---
    settings.texts.forEach { text ->
        val isChip = text.backgroundStyle !is TextBackgroundStyle.None

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isChip) android.graphics.Color.WHITE else text.color.toArgb()
            isFakeBoldText = isChip
            // Так как в UI используется .sp, реальный размер в пикселях = fontSize * density
            textSize = text.fontSize * screenDensity * scaleFactor
            typeface = getAndroidTypeface(context, text.fontFamily)
        }

        val posX = ((text.position.x - offsetX) * scaleFactor) - left
        val posY = ((text.position.y - offsetY) * scaleFactor) - top

        // Ширина текста также должна учитывать масштабирование
        val textWidth = textPaint.measureText(text.text).toInt().coerceAtLeast(1)

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text.text, 0, text.text.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            StaticLayout(text.text, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
        }

        canvas.withSave {
            translate(posX, posY)

            // Чип-подложка под текстом — цвет/градиент зависит от выбранного backgroundStyle
            if (isChip) {
                val paddingH = 14.dp.toPxRaw(context) * scaleFactor
                val paddingV = 6.dp.toPxRaw(context) * scaleFactor

                val chipRect = RectF(
                    -paddingH,
                    -paddingV,
                    staticLayout.width.toFloat() + paddingH,
                    staticLayout.height.toFloat() + paddingV
                )

                val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    when (val style = text.backgroundStyle) {
                        is TextBackgroundStyle.Solid -> {
                            color = style.color.toArgb()
                        }
                        is TextBackgroundStyle.Gradient -> {
                            shader = LinearGradient(
                                chipRect.left, 0f, chipRect.right, 0f,
                                style.start.toArgb(), style.end.toArgb(),
                                Shader.TileMode.CLAMP
                            )
                        }
                        is TextBackgroundStyle.None -> { /* сюда не попадём, isChip уже false */ }
                    }
                }

                val cornerRadius = chipRect.height() / 2f
                drawRoundRect(chipRect, cornerRadius, cornerRadius, chipPaint)
            }

            staticLayout.draw(this)
        }
    }

    // --- ШАГ 5: Сохранение ---
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

    croppedBitmap.recycle()
    processedBitmap.recycle()
    onComplete(uri)
}

// Вспомогательная функция для перевода dp в реальные экранные px
private fun Dp.toPxRaw(context: Context): Float =
    this.value * context.resources.displayMetrics.density

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