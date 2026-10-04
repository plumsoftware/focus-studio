package ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Что именно тянет палец. Определяется ОДИН раз — в момент начала жеста,
 * а не на каждом кадре, поэтому рамка не «перескакивает» между краями.
 *
 * [hx]: -1 — левый край, +1 — правый, 0 — не трогаем по горизонтали.
 * [vy]: -1 — верхний край, +1 — нижний, 0 — не трогаем по вертикали.
 * Оба нуля при [move] = true — перенос всей рамки.
 */
private data class CropHandle(val hx: Int, val vy: Int, val move: Boolean)

/**
 * Рамка кадрирования поверх фото.
 *
 * @param rect рамка в долях ИЗОБРАЖЕНИЯ (0..1), а не контейнера
 * @param imageBounds где на экране лежит само фото (в пикселях контейнера)
 * @param aspectRatio пропорции результата в пикселях (ширина / высота) или null — свободно
 */
@Composable
fun AdvancedCropOverlay(
    rect: Rect,
    imageBounds: Rect,
    aspectRatio: Float?,
    onRectChange: (Rect) -> Unit
) {
    val currentRect by rememberUpdatedState(rect)
    val currentBounds by rememberUpdatedState(imageBounds)
    val currentRatio by rememberUpdatedState(aspectRatio)
    val currentOnRectChange by rememberUpdatedState(onRectChange)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val touchZone = 28.dp.toPx()
                val minSidePx = 56.dp.toPx()
                var handle: CropHandle? = null

                detectDragGestures(
                    onDragStart = { start ->
                        handle = findHandle(start, toScreen(currentRect, currentBounds), touchZone)
                    },
                    onDragEnd = { handle = null },
                    onDragCancel = { handle = null },
                    onDrag = { change, drag ->
                        val h = handle ?: return@detectDragGestures
                        val bounds = currentBounds
                        if (bounds.width <= 0f || bounds.height <= 0f) return@detectDragGestures
                        change.consume()

                        val dx = drag.x / bounds.width
                        val dy = drag.y / bounds.height
                        val minW = (minSidePx / bounds.width).coerceAtMost(1f)
                        val minH = (minSidePx / bounds.height).coerceAtMost(1f)
                        // Пропорции рамки в нормализованных координатах
                        val normalizedRatio = currentRatio?.let {
                            it / (bounds.width / bounds.height)
                        }

                        currentOnRectChange(
                            if (h.move) moveRect(currentRect, dx, dy)
                            else resizeRect(currentRect, h, dx, dy, normalizedRatio, minW, minH)
                        )
                    }
                )
            }
    ) {
        val r = toScreen(rect, imageBounds)

        // Затемнение всего, что будет отрезано
        val dim = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(r)
            fillType = PathFillType.EvenOdd
        }
        drawPath(dim, Color.Black.copy(alpha = 0.7f))

        // Сетка третей
        val gridColor = Color.White.copy(alpha = 0.35f)
        val gridStroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = r.left + r.width * i / 3f
            val y = r.top + r.height * i / 3f
            drawLine(gridColor, Offset(x, r.top), Offset(x, r.bottom), gridStroke)
            drawLine(gridColor, Offset(r.left, y), Offset(r.right, y), gridStroke)
        }

        // Рамка
        drawRect(Color.White, topLeft = r.topLeft, size = r.size, style = Stroke(width = 1.5.dp.toPx()))

        val thick = 4.dp.toPx()
        val radius = CornerRadius(thick / 2f)

        // Ручки по серединам сторон: верх, низ, лево, право
        val edgeLen = minOf(32.dp.toPx(), r.width / 3f, r.height / 3f)
        drawRoundRect(Color.White, Offset(r.center.x - edgeLen / 2, r.top - thick / 2), Size(edgeLen, thick), radius)
        drawRoundRect(Color.White, Offset(r.center.x - edgeLen / 2, r.bottom - thick / 2), Size(edgeLen, thick), radius)
        drawRoundRect(Color.White, Offset(r.left - thick / 2, r.center.y - edgeLen / 2), Size(thick, edgeLen), radius)
        drawRoundRect(Color.White, Offset(r.right - thick / 2, r.center.y - edgeLen / 2), Size(thick, edgeLen), radius)

        // Угловые ручки-уголки
        val cornerLen = minOf(20.dp.toPx(), r.width / 4f, r.height / 4f)
        for (sx in listOf(-1, 1)) {
            for (sy in listOf(-1, 1)) {
                val cx = if (sx < 0) r.left else r.right
                val cy = if (sy < 0) r.top else r.bottom
                // горизонтальная часть уголка
                drawRoundRect(
                    Color.White,
                    Offset(if (sx < 0) cx - thick / 2 else cx - cornerLen + thick / 2, cy - thick / 2),
                    Size(cornerLen, thick),
                    radius
                )
                // вертикальная часть уголка
                drawRoundRect(
                    Color.White,
                    Offset(cx - thick / 2, if (sy < 0) cy - thick / 2 else cy - cornerLen + thick / 2),
                    Size(thick, cornerLen),
                    radius
                )
            }
        }
    }
}

/** Рамка из долей изображения -> в пиксели экрана. */
private fun toScreen(rect: Rect, bounds: Rect) = Rect(
    bounds.left + rect.left * bounds.width,
    bounds.top + rect.top * bounds.height,
    bounds.left + rect.right * bounds.width,
    bounds.top + rect.bottom * bounds.height
)

/**
 * За что взялся палец. Зона захвата края — [zone] наружу от рамки и не больше
 * трети стороны внутрь, чтобы у маленькой рамки оставалась середина для переноса.
 */
private fun findHandle(touch: Offset, r: Rect, zone: Float): CropHandle? {
    val insideX = minOf(zone, r.width / 3f)
    val insideY = minOf(zone, r.height / 3f)

    val withinX = touch.x >= r.left - zone && touch.x <= r.right + zone
    val withinY = touch.y >= r.top - zone && touch.y <= r.bottom + zone
    if (!withinX || !withinY) return null

    val hx = when {
        touch.x <= r.left + insideX -> -1
        touch.x >= r.right - insideX -> 1
        else -> 0
    }
    val vy = when {
        touch.y <= r.top + insideY -> -1
        touch.y >= r.bottom - insideY -> 1
        else -> 0
    }
    return CropHandle(hx, vy, move = hx == 0 && vy == 0)
}

private fun moveRect(rect: Rect, dx: Float, dy: Float): Rect {
    val l = (rect.left + dx).coerceIn(0f, (1f - rect.width).coerceAtLeast(0f))
    val t = (rect.top + dy).coerceIn(0f, (1f - rect.height).coerceAtLeast(0f))
    return Rect(l, t, l + rect.width, t + rect.height)
}

/**
 * Изменение размера. Все координаты — доли изображения (0..1).
 * [ratio] — нужное отношение (доля ширины / доля высоты) либо null для свободной рамки.
 */
private fun resizeRect(
    rect: Rect,
    handle: CropHandle,
    dx: Float,
    dy: Float,
    ratio: Float?,
    minW: Float,
    minH: Float
): Rect {
    val hx = handle.hx
    val vy = handle.vy

    // ---------- Свободная рамка: каждый край двигается сам по себе ----------
    if (ratio == null) {
        var l = rect.left
        var t = rect.top
        var r = rect.right
        var b = rect.bottom
        if (hx < 0) l = (l + dx).coerceIn(0f, (r - minW).coerceAtLeast(0f))
        if (hx > 0) r = (r + dx).coerceIn((l + minW).coerceAtMost(1f), 1f)
        if (vy < 0) t = (t + dy).coerceIn(0f, (b - minH).coerceAtLeast(0f))
        if (vy > 0) b = (b + dy).coerceIn((t + minH).coerceAtMost(1f), 1f)
        return Rect(l, t, r, b)
    }

    // ---------- Фиксированные пропорции ----------
    // Угол: противоположный угол остаётся на месте.
    if (hx != 0 && vy != 0) {
        val anchorX = if (hx > 0) rect.left else rect.right
        val anchorY = if (vy > 0) rect.top else rect.bottom
        val growFromX = hx * dx
        val growFromY = vy * dy * ratio
        val grow = if (kotlin.math.abs(growFromX) >= kotlin.math.abs(growFromY)) growFromX else growFromY

        val roomW = if (hx > 0) 1f - anchorX else anchorX
        val roomH = if (vy > 0) 1f - anchorY else anchorY
        val maxW = minOf(roomW, roomH * ratio)
        val lowW = minOf(maxOf(minW, minH * ratio), maxW)
        val w = (rect.width + grow).coerceIn(lowW, maxW)
        val h = w / ratio

        val l = if (hx > 0) anchorX else anchorX - w
        val t = if (vy > 0) anchorY else anchorY - h
        return Rect(l, t, l + w, t + h)
    }

    // Левый/правый край: противоположный край на месте, высота меняется от центра.
    if (hx != 0) {
        val anchorX = if (hx > 0) rect.left else rect.right
        val centerY = rect.center.y
        val roomW = if (hx > 0) 1f - anchorX else anchorX
        val roomH = 2f * minOf(centerY, 1f - centerY)
        val maxW = minOf(roomW, roomH * ratio)
        val lowW = minOf(maxOf(minW, minH * ratio), maxW)
        val w = (rect.width + hx * dx).coerceIn(lowW, maxW)
        val h = w / ratio

        val l = if (hx > 0) anchorX else anchorX - w
        return Rect(l, centerY - h / 2f, l + w, centerY + h / 2f)
    }

    // Верхний/нижний край: противоположный край на месте, ширина меняется от центра.
    val anchorY = if (vy > 0) rect.top else rect.bottom
    val centerX = rect.center.x
    val roomH = if (vy > 0) 1f - anchorY else anchorY
    val roomW = 2f * minOf(centerX, 1f - centerX)
    val maxH = minOf(roomH, roomW / ratio)
    val lowH = minOf(maxOf(minH, minW / ratio), maxH)
    val h = (rect.height + vy * dy).coerceIn(lowH, maxH)
    val w = h * ratio

    val t = if (vy > 0) anchorY else anchorY - h
    return Rect(centerX - w / 2f, t, centerX + w / 2f, t + h)
}