package ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun AdvancedCropOverlay(
    currentSettings: PhotoSettings,
    onCropApply: (Rect) -> Unit
) {
    // Синхронизируем локальный rect с тем, что приходит из пресетов
    var rect by remember(currentSettings.cropRect) { mutableStateOf(currentSettings.cropRect) }
    val ratio = currentSettings.aspectRatio

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentSettings.cropRect) { // Перезапуск при смене пресета
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / w
                    val dy = dragAmount.y / h
                    val x = change.position.x / w
                    val y = change.position.y / h

                    val threshold = 0.12f // Зона захвата краев

                    rect = when {
                        // ТЯНЕМ ПРАВЫЙ КРАЙ
                        x > rect.right - threshold -> {
                            val newRight = (rect.right + dx).coerceIn(rect.left + 0.1f, 1f)
                            if (ratio != null) {
                                // Если есть пресет, меняем высоту пропорционально
                                val newHeight = (newRight - rect.left) / ratio
                                rect.copy(right = newRight, bottom = (rect.top + newHeight).coerceAtMost(1f))
                            } else {
                                rect.copy(right = newRight)
                            }
                        }
                        // ТЯНЕМ ЛЕВЫЙ КРАЙ
                        x < rect.left + threshold -> {
                            val newLeft = (rect.left + dx).coerceIn(0f, rect.right - 0.1f)
                            if (ratio != null) {
                                val newHeight = (rect.right - newLeft) / ratio
                                rect.copy(left = newLeft, bottom = (rect.top + newHeight).coerceAtMost(1f))
                            } else {
                                rect.copy(left = newLeft)
                            }
                        }
                        // ТЯНЕМ НИЖНИЙ КРАЙ
                        y > rect.bottom - threshold -> {
                            val newBottom = (rect.bottom + dy).coerceIn(rect.top + 0.1f, 1f)
                            if (ratio != null) {
                                val newWidth = (newBottom - rect.top) * ratio
                                rect.copy(bottom = newBottom, right = (rect.left + newWidth).coerceAtMost(1f))
                            } else {
                                rect.copy(bottom = newBottom)
                            }
                        }
                        // ПЕРЕМЕЩЕНИЕ ВСЕЙ РАМКИ
                        else -> {
                            val newL = (rect.left + dx).coerceIn(0f, 1f - rect.width)
                            val newT = (rect.top + dy).coerceIn(0f, 1f - rect.height)
                            Rect(newL, newT, newL + rect.width, newT + rect.height)
                        }
                    }
                }
            }
        ) {
            val r = Rect(rect.left * w, rect.top * h, rect.right * w, rect.bottom * h)

            // Затемнение фона
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRect(r)
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, Color.Black.copy(alpha = 0.7f))

            // Рамка (iOS Style)
            drawRect(Color.White, topLeft = r.topLeft, size = r.size, style = Stroke(width = 2.dp.toPx()))

            // Ручки-овалы (индикаторы интерактивности)
            val hLen = 32.dp.toPx(); val hThick = 4.dp.toPx()
            val hColor = Color.White
            // Верх, Низ, Лево, Право
            drawRoundRect(hColor, Offset(r.center.x - hLen/2, r.top - hThick/2), Size(hLen, hThick), CornerRadius(hThick))
            drawRoundRect(hColor, Offset(r.center.x - hLen/2, r.bottom - hThick/2), Size(hLen, hThick), CornerRadius(hThick))
            drawRoundRect(hColor, Offset(r.left - hThick/2, r.center.y - hLen/2), Size(hThick, hLen), CornerRadius(hThick))
            drawRoundRect(hColor, Offset(r.right - hThick/2, r.center.y - hLen/2), Size(hThick, hLen), CornerRadius(hThick))
        }

        // Кнопка подтверждения
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .clickable { onCropApply(rect) },
            color = iOSBlue,
            shape = CircleShape
        ) {
            Text(
                stringResource(R.string.btn_crop_apply).uppercase(),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}