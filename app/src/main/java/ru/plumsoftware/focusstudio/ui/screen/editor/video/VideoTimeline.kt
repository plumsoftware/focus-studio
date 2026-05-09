package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.DragSource
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun VideoTimeline(
    settings: VideoSettings,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    onRangeChange: (Long, Long) -> Unit
) {
    key(settings.durationMs) {
        val duration = settings.durationMs.coerceAtLeast(1L)
        val actualEndMs = if (settings.endMs == 0L) settings.durationMs else settings.endMs

        val localStartMs = remember { mutableLongStateOf(settings.startMs) }
        val localEndMs = remember { mutableLongStateOf(actualEndMs) }
        val localTrackerPos = remember { mutableLongStateOf(currentPosition) }

        // Флаги перетаскивания (для показа бабла)
        var isDraggingTracker by remember { mutableStateOf(false) }
        var isDraggingHandle by remember { mutableStateOf(false) }
        val isAnyDragging = isDraggingTracker || isDraggingHandle

        LaunchedEffect(currentPosition) {
            if (!isAnyDragging) {
                localTrackerPos.longValue = currentPosition
            }
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatTimeSmart(localTrackerPos.longValue, duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    formatTimeSmart(duration, duration),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp) // Увеличили высоту контейнера под бабл
            ) {
                val widthPx = constraints.maxWidth.toFloat()
                val density = LocalDensity.current

                fun pxToMs(px: Float) = (px / widthPx * duration).toLong().coerceIn(0, duration)
                fun msToPx(ms: Long) =
                    (ms.toFloat() / duration.toFloat()).coerceIn(0f, 1f) * widthPx

                val startPx = msToPx(localStartMs.longValue)
                val endPx = msToPx(localEndMs.longValue)
                val trackerPx = msToPx(localTrackerPos.longValue)

                // --- ИНФО-БАБЛ ---
                // Показываем только если что-то тянем
                androidx.compose.animation.AnimatedVisibility(
                    visible = isAnyDragging,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .offset {
                            // Центрируем бабл над трекером и ограничиваем краями экрана
                            val bubbleWidth = 60.dp.toPx()
                            val xOff = (trackerPx - bubbleWidth / 2)
                                .coerceIn(0f, widthPx - bubbleWidth)
                            IntOffset(xOff.toInt(), 0)
                        }
                ) {
                    Surface(
                        color = Color(0xFF007AFF), // Цвет iOS Blue
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.shadow(elevation = 4.dp)
                    ) {
                        Text(
                            text = formatTimeSmart(localTrackerPos.longValue, duration),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Вложенный контейнер для самого таймлайна (смещен вниз под бабл)
                Box(Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)) {

                    // 1. Фон
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .align(Alignment.Center)
                            .background(Color.White.copy(0.1f), CircleShape)
                    )

                    // 2. Синяя зона
                    val regionWidth = (endPx - startPx).coerceAtLeast(0f)
                    Box(
                        Modifier
                            .offset { IntOffset(startPx.toInt(), 16.dp.run { toPx().toInt() }) }
                            .width(with(density) { regionWidth.toDp() })
                            .height(32.dp)
                            .background(Color(0xFF007AFF).copy(0.2f))
                            .border(2.dp, Color(0xFF007AFF), RoundedCornerShape(4.dp))
                    )

                    // 3. Левая ручка
                    Handle(offsetPx = startPx) { deltaX ->
                        isDraggingHandle = true
                        val newStart = pxToMs(msToPx(localStartMs.longValue) + deltaX)
                            .coerceAtMost(localEndMs.longValue - 500)
                        localStartMs.longValue = newStart
                        localTrackerPos.longValue = newStart
                        onSeek(newStart)
                        onRangeChange(newStart, localEndMs.longValue)
                    }

                    // 4. Правая ручка
                    Handle(offsetPx = endPx) { deltaX ->
                        isDraggingHandle = true
                        val newEnd = pxToMs(msToPx(localEndMs.longValue) + deltaX)
                            .coerceAtLeast(localStartMs.longValue + 500)
                        localEndMs.longValue = newEnd
                        localTrackerPos.longValue = newEnd
                        onSeek(newEnd)
                        onRangeChange(localStartMs.longValue, newEnd)
                    }

                    // 5. ТРЕКЕР
                    val trackerHeight = 64.dp
                    val circleSize = 12.dp

                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    trackerPx.toInt() - (circleSize.toPx() / 2).toInt(),
                                    ((64.dp - trackerHeight) / 2).run { toPx().toInt() }
                                )
                            }
                            .width(circleSize)
                            .height(trackerHeight)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDraggingTracker = true },
                                    onDragEnd = {
                                        isDraggingTracker = false; isDraggingHandle = false
                                    },
                                    onDragCancel = {
                                        isDraggingTracker = false; isDraggingHandle = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newPos =
                                            pxToMs(msToPx(localTrackerPos.longValue) + dragAmount.x)
                                                .coerceIn(
                                                    localStartMs.longValue,
                                                    localEndMs.longValue
                                                )
                                        localTrackerPos.longValue = newPos
                                        onSeek(newPos)
                                    }
                                )
                            }
                    ) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .align(Alignment.Center)
                                .background(Color.White)
                        )
                        Box(
                            Modifier
                                .size(circleSize)
                                .align(Alignment.TopCenter)
                                .background(Color.White, CircleShape)
                        )
                        Box(
                            Modifier
                                .size(circleSize)
                                .align(Alignment.BottomCenter)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// Добавим в Handle сброс флага
@Composable
private fun Handle(offsetPx: Float, onDrag: (Float) -> Unit) {
    Box(
        Modifier
            .offset { IntOffset(offsetPx.toInt() - 20, 12.dp.run { toPx().toInt() }) }
            .size(40.dp, 40.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = { /* Флаг сбросится в родителе */ },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier
            .size(6.dp, 24.dp)
            .background(Color.White, CircleShape))
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

@SuppressLint("DefaultLocale")
fun formatTimeSmart(ms: Long, totalDurationMs: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (totalDurationMs >= 3600_000) {
        // Если видео больше часа: HH:MM:SS
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        // Если видео меньше часа: MM:SS
        String.format("%02d:%02d", minutes, seconds)
    }
}