package ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.addArrow
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.addStar
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeElement
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType
import ru.plumsoftware.focusstudio.ui.theme.AccentStart
import kotlin.math.roundToInt

@Composable
fun ShapeComponent(
    shape: ShapeElement,
    isSelected: Boolean,
    onCommitTransform: (ShapeElement) -> Unit,
    onClick: () -> Unit
) {
    // ГАРАНТИЯ: Всегда видим актуальные цвета, поворот и тип фигуры
    val currentShapeState by rememberUpdatedState(shape)
    // Жесты живут дольше одной рекомпозиции — всегда вызываем актуальный колбэк,
    // иначе фиксация перетаскивания затирала настройки устаревшим снимком.
    val currentOnCommit by rememberUpdatedState(onCommitTransform)
    val density = LocalDensity.current.density

    // ЛОКАЛЬНОЕ СОСТОЯНИЕ: залог плавности 60 FPS
    var localOffset by remember(shape.id) { mutableStateOf(shape.position) }
    var localSize by remember(shape.id) { mutableStateOf(shape.size) }

    // СИНХРОНИЗАЦИЯ: для корректной работы Undo/Redo
    LaunchedEffect(shape.position, shape.size) {
        localOffset = shape.position
        localSize = shape.size
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(localOffset.x.roundToInt(), localOffset.y.roundToInt()) }
            .size(localSize.width.dp, localSize.height.dp)
            .graphicsLayer(rotationZ = shape.rotation)
    ) {
        // Рендеринг самой фигуры
        Canvas(modifier = Modifier.fillMaxSize().clickable { onClick() }) {
            val path = Path().apply {
                when (shape.type) {
                    ShapeType.SQUARE -> addRect(Rect(Offset.Zero, size))
                    ShapeType.CIRCLE -> addOval(Rect(Offset.Zero, size))
                    ShapeType.TRIANGLE -> {
                        moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                    }
                    ShapeType.STAR -> addStar(size, 5, size.width / 2, size.width / 4)
                    ShapeType.ARROW -> addArrow(size)
                }
            }
            if (shape.fillColor != Color.Transparent) drawPath(path, shape.fillColor)
            if (shape.strokeColor != Color.Transparent) drawPath(path, shape.strokeColor, style = Stroke(2.dp.toPx()))
        }

        if (isSelected) {
            // Рамка выделения
            Box(modifier = Modifier.fillMaxSize().border(1.5.dp, AccentStart, RoundedCornerShape(2.dp)))

            // ПЕРЕТАСКИВАНИЕ (Локальное и плавное)
            Box(modifier = Modifier.fillMaxSize().pointerInput(shape.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localOffset += dragAmount
                    },
                    onDragEnd = {
                        // Только тут фиксируем в историю, сохраняя все текущие настройки цвета/поворота
                        currentOnCommit(currentShapeState.copy(position = localOffset))
                    }
                )
            })

            // ИЗМЕНЕНИЕ РАЗМЕРА: заметная цветная ручка со стрелкой «растянуть» в углу
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
                    .offset(16.dp, 16.dp)
                    .shadow(4.dp, CircleShape)
                    .background(AccentStart, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(shape.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Размер хранится в dp, а палец двигается в пикселях —
                                // переводим, чтобы угол фигуры шёл точно за пальцем.
                                val newW = (localSize.width + dragAmount.x / density).coerceAtLeast(30f)
                                val newH = (localSize.height + dragAmount.y / density).coerceAtLeast(30f)
                                localSize = Size(newW, newH)
                            },
                            onDragEnd = {
                                // Только тут фиксируем в историю
                                currentOnCommit(currentShapeState.copy(size = localSize))
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = null,
                    tint = Color.White,
                    // Иконка по умолчанию смотрит в другую диагональ — разворачиваем
                    // вдоль направления «левый верх — правый низ»
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer(rotationZ = 90f)
                )
            }
        }
    }
}