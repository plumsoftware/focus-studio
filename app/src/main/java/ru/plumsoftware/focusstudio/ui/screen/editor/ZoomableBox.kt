package ru.plumsoftware.focusstudio.ui.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Масштаб предпросмотра. Это только «лупа» для удобной работы: на сохранённый
 * файл не влияет — координаты текста, фигур и рамки обрезки считаются внутри
 * увеличенного слоя и от масштаба не зависят.
 */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean get() = scale > 1.01f

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** Щипок: масштабируем вокруг точки между пальцами и одновременно сдвигаем. */
    fun transform(centroid: Offset, pan: Offset, zoom: Float, size: IntSize) {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val factor = newScale / scale
        // Точка под пальцами остаётся под пальцами
        val newOffset = centroid - (centroid - offset) * factor + pan
        scale = newScale
        offset = clamp(newOffset, newScale, size)
    }

    fun panBy(pan: Offset, size: IntSize) {
        offset = clamp(offset + pan, scale, size)
    }

    /** Не даём утащить содержимое за край: пустых полей по бокам не появляется. */
    private fun clamp(value: Offset, scale: Float, size: IntSize): Offset {
        val minX = -size.width * (scale - 1f)
        val minY = -size.height * (scale - 1f)
        return Offset(value.x.coerceIn(minX, 0f), value.y.coerceIn(minY, 0f))
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 6f
    }
}

@Composable
fun rememberZoomState(): ZoomState = remember { ZoomState() }

/**
 * Контейнер с увеличением:
 *  - двумя пальцами — увеличить/уменьшить и одновременно двигать;
 *  - одним пальцем по свободному месту — листать увеличенную область;
 *  - кнопка с кратностью в углу — вернуть обычный вид.
 *
 * [content] рисуется внутри увеличенного слоя, [overlay] — поверх, без увеличения
 * (например, кнопка воспроизведения).
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    state: ZoomState = rememberZoomState(),
    contentAlignment: Alignment = Alignment.TopStart,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clipToBounds()
            // Два пальца. Работаем на проходе Initial — раньше дочерних элементов,
            // чтобы щипок не превращался в перетаскивание текста, фигуры или рамки обрезки.
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            state.transform(
                                centroid = event.calculateCentroid(useCurrent = false),
                                pan = event.calculatePan(),
                                zoom = event.calculateZoom(),
                                size = size
                            )
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            // Один палец: листаем увеличенную область. Срабатывает только если жест
            // не забрал дочерний элемент (текст, фигура, рамка обрезки).
            .pointerInput(state) {
                detectDragGestures { change, dragAmount ->
                    if (state.isZoomed) {
                        change.consume()
                        state.panBy(dragAmount, size)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                },
            contentAlignment = contentAlignment,
            content = content
        )

        overlay()

        // Кратность + сброс. Видна только при увеличении.
        if (state.isZoomed) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { state.reset() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f×", state.scale),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(14.dp)
                )
            }
        }
    }
}