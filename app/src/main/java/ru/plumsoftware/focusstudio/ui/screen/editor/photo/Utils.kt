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
import android.media.ExifInterface
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

/**
 * Рамка кадрирования под выбранный пресет.
 *
 * ВАЖНО: cropRect хранится в долях ИЗОБРАЖЕНИЯ (0..1 по ширине и высоте фото),
 * а [ratio] — это пропорции результата в пикселях (ширина / высота). Поэтому
 * для расчёта нужны пропорции самого фото [imageAspect]: иначе «1:1» на
 * вытянутом снимке получался не квадратом.
 *
 * Возвращает максимально большую рамку нужных пропорций по центру фото.
 */
fun calculateRectForRatio(ratio: Float, imageAspect: Float): Rect {
    // Пропорции рамки в нормализованных координатах (доля ширины / доля высоты)
    val normalized = ratio / imageAspect
    return if (normalized >= 1f) {
        val h = 1f / normalized
        Rect(0f, 0.5f - h / 2f, 1f, 0.5f + h / 2f)
    } else {
        Rect(0.5f - normalized / 2f, 0f, 0.5f + normalized / 2f, 1f)
    }
}

/**
 * Где на экране реально лежит фото, вписанное (ContentScale.Fit) в контейнер.
 * Контейнер почти никогда не совпадает с фото по пропорциям — по краям остаются поля.
 */
fun fittedImageRect(container: Size, imageAspect: Float): Rect {
    if (container.width <= 0f || container.height <= 0f || imageAspect <= 0f) {
        return Rect(0f, 0f, container.width, container.height)
    }
    val containerAspect = container.width / container.height
    val w: Float
    val h: Float
    if (imageAspect > containerAspect) {
        w = container.width
        h = w / imageAspect
    } else {
        h = container.height
        w = h * imageAspect
    }
    val left = (container.width - w) / 2f
    val top = (container.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

/**
 * Как показать обрезанное фото: выбранный фрагмент равномерно (без искажений)
 * увеличивается и ставится по центру контейнера.
 * Преобразование: экран = точка * scale + (tx, ty), начало координат — левый верхний угол.
 */
data class CropViewTransform(
    val scale: Float,
    val tx: Float,
    val ty: Float,
    /** Обрезаемая область в координатах контейнера ДО преобразования. */
    val cropInContainer: Rect
)

fun cropViewTransform(container: Size, imageAspect: Float, crop: Rect): CropViewTransform {
    val fitted = fittedImageRect(container, imageAspect)
    val region = Rect(
        fitted.left + crop.left * fitted.width,
        fitted.top + crop.top * fitted.height,
        fitted.left + crop.right * fitted.width,
        fitted.top + crop.bottom * fitted.height
    )
    if (region.width <= 0f || region.height <= 0f) {
        return CropViewTransform(1f, 0f, 0f, region)
    }
    val scale = minOf(container.width / region.width, container.height / region.height)
    val tx = (container.width - region.width * scale) / 2f - region.left * scale
    val ty = (container.height - region.height * scale) / 2f - region.top * scale
    return CropViewTransform(scale, tx, ty, region)
}

/** Читает фото и поворачивает его по EXIF — так же, как его показывает редактор. */
private fun decodeBitmapWithOrientation(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null

    val orientation = try {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
    // Фото читаем с учётом EXIF-поворота: редактор показывает его уже повёрнутым,
    // и без этого на снимках с камеры вырезался не тот кусок.
    val originalBitmap = decodeBitmapWithOrientation(context, originalUri)
    if (originalBitmap == null) {
        onComplete(null)
        return
    }

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
    // cropRect — в долях самого фото, поэтому просто умножаем на размер файла.
    val crop = settings.cropRect
    val left = (bitmapWidth * crop.left).toInt().coerceIn(0, originalBitmap.width - 1)
    val top = (bitmapHeight * crop.top).toInt().coerceIn(0, originalBitmap.height - 1)
    val width = (bitmapWidth * (crop.right - crop.left)).toInt()
        .coerceIn(1, originalBitmap.width - left)
    val height = (bitmapHeight * (crop.bottom - crop.top)).toInt()
        .coerceIn(1, originalBitmap.height - top)

    // На экране обрезанное фото показано увеличенным (см. cropViewTransform), а текст и
    // фигуры стоят в координатах экрана. Возвращаем их в координаты фото тем же преобразованием.
    val view = cropViewTransform(
        Size(containerWidth, containerHeight),
        bitmapWidth / bitmapHeight,
        crop
    )
    // Экранные пиксели увеличенного вида -> пиксели файла
    val elementScale = scaleFactor / view.scale
    fun toFileX(screenX: Float) = ((screenX - view.tx) / view.scale - offsetX) * scaleFactor - left
    fun toFileY(screenY: Float) = ((screenY - view.ty) / view.scale - offsetY) * scaleFactor - top

    val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height)
    val processedBitmap = applyEffectsToBitmap(context, croppedBitmap, settings)

    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(processedBitmap, 0f, 0f, null)

    // --- ШАГ 3: Отрисовка ФИГУР ---
    settings.shapes.forEach { shape ->
        canvas.withSave {
            val posX = toFileX(shape.position.x)
            val posY = toFileY(shape.position.y)

            // Размер фигуры задан в dp
            val shapeW = shape.size.width * screenDensity * elementScale
            val shapeH = shape.size.height * screenDensity * elementScale

            translate(posX, posY)
            // Вращаем вокруг центра фигуры — так же, как она показана на экране
            rotate(shape.rotation, shapeW / 2f, shapeH / 2f)
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
                    strokeWidth = 2f * screenDensity * elementScale
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
            textSize = text.fontSize * screenDensity * elementScale
            typeface = getAndroidTypeface(context, text.fontFamily)
        }

        val posX = toFileX(text.position.x)
        val posY = toFileY(text.position.y)

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
                val paddingH = 14.dp.toPxRaw(context) * elementScale
                val paddingV = 6.dp.toPxRaw(context) * elementScale

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