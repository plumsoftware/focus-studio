package ru.plumsoftware.focusstudio.ui.screen.editor.photo.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Сборка коллажа в один bitmap и запись во временный файл кэша.
 *
 * ВАЖНО: это НЕ трогает основной способ сохранения (saveEditedImage). Здесь мы лишь
 * готовим единое изображение-коллаж, которое дальше открывается в обычном фоторедакторе
 * и сохраняется тем же проверенным путём. Каждое фото декодируется с down-sampling
 * под размер своей ячейки — память остаётся скромной даже на слабых устройствах.
 */

// Максимальная сторона итогового коллажа. Компромисс между качеством и памятью.
private const val MAX_OUTPUT_DIM = 1440

fun computeOutputSize(aspect: CollageAspect): Pair<Int, Int> {
    val ratio = aspect.ratio
    return if (ratio >= 1f) {
        MAX_OUTPUT_DIM to (MAX_OUTPUT_DIM / ratio).toInt()
    } else {
        (MAX_OUTPUT_DIM * ratio).toInt() to MAX_OUTPUT_DIM
    }
}

/**
 * Собирает коллаж и возвращает Uri файла в кэше (file://), либо null при ошибке.
 * Вызывать на фоновом потоке (Dispatchers.IO).
 */
fun composeCollageToCache(context: Context, state: CollageState): Uri? {
    return try {
        val (outW, outH) = computeOutputSize(state.aspect)
        if (outW <= 0 || outH <= 0) return null

        val result = createBitmap(outW, outH)
        val canvas = Canvas(result)

        // Фон
        canvas.drawColor(state.background.toArgb())

        val minDim = minOf(outW, outH).toFloat()
        val gapPx = state.gap * 0.05f * minDim
        val cornerPx = state.corner * 0.10f * minDim
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

        state.template.cells.forEachIndexed { index, rect ->
            val cell = state.cells.getOrNull(index) ?: CollageCell()

            val cellRect = RectF(
                rect.left * outW + gapPx / 2f,
                rect.top * outH + gapPx / 2f,
                rect.right * outW - gapPx / 2f,
                rect.bottom * outH - gapPx / 2f
            )
            if (cellRect.width() <= 1f || cellRect.height() <= 1f) return@forEachIndexed

            val clipPath = Path().apply {
                addRoundRect(cellRect, cornerPx, cornerPx, Path.Direction.CW)
            }

            canvas.save()
            canvas.clipPath(clipPath)

            val uri = cell.uri
            if (uri == null) {
                // Пустая ячейка — лёгкая подложка, чтобы место было видно.
                canvas.drawColor(0x22FFFFFF)
            } else {
                val cw = cellRect.width()
                val ch = cellRect.height()
                val bmp = decodeSampledBitmap(context, uri, cw.toInt(), ch.toInt())
                if (bmp != null) {
                    drawCellBitmap(canvas, bmp, cellRect, cell, paint)
                    bmp.recycle()
                }
            }
            canvas.restore()
        }

        val uri = saveBitmapToCache(context, result)
        result.recycle()
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Рисует фото внутри ячейки: cover-fill по центру + пользовательский зум/сдвиг.
 * Эта же математика (центр + зум + сдвиг в долях ячейки) повторяется в превью на Compose,
 * поэтому предпросмотр и результат совпадают.
 */
private fun drawCellBitmap(
    canvas: Canvas,
    bmp: Bitmap,
    cellRect: RectF,
    cell: CollageCell,
    paint: Paint
) {
    val sw = bmp.width.toFloat()
    val sh = bmp.height.toFloat()
    if (sw <= 0f || sh <= 0f) return

    val cw = cellRect.width()
    val ch = cellRect.height()

    val coverScale = max(cw / sw, ch / sh)
    val scale = coverScale * cell.zoom
    val drawW = sw * scale
    val drawH = sh * scale

    // Сдвиг ограничиваем так, чтобы фото всегда покрывало ячейку (без «дыр»).
    val clamp = max(0f, (cell.zoom - 1f) / 2f)
    val panX = cell.panX.coerceIn(-clamp, clamp)
    val panY = cell.panY.coerceIn(-clamp, clamp)

    val dx = cellRect.left + (cw - drawW) / 2f + panX * cw
    val dy = cellRect.top + (ch - drawH) / 2f + panY * ch

    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
    }
    canvas.drawBitmap(bmp, matrix, paint)
}

private fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    reqW: Int,
    reqH: Int
): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val opts = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
    var sample = 1
    val targetW = reqW.coerceAtLeast(1)
    val targetH = reqH.coerceAtLeast(1)
    var halfW = srcW / 2
    var halfH = srcH / 2
    while (halfW / sample >= targetW && halfH / sample >= targetH) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    val file = File(context.cacheDir, "collage_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    return file.toUri()
}
