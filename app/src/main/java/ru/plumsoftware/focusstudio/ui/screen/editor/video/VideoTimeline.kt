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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    val duration = settings.durationMs.coerceAtLeast(1L)
    val density = LocalDensity.current.density
    val actualEndMs = if (settings.endMs == 0L) settings.durationMs else settings.endMs

    // 1. Локальные переменные — "единственный источник правды" во время жеста
    var localStartMs by remember { mutableLongStateOf(settings.startMs) }
    var localEndMs by remember { mutableLongStateOf(actualEndMs) }
    var localTrackerPos by remember { mutableLongStateOf(currentPosition) }

    // Флаг: если мы хоть что-то тянем, мы ЗАПРЕЩАЕМ внешним данным обновлять UI
    var isInteracting by remember { mutableStateOf(false) }

    // 2. Синхронизация: обновляем локальные данные только тогда, когда пользователь НЕ ТРОГАЕТ экран
    LaunchedEffect(settings.startMs, actualEndMs, currentPosition) {
        if (!isInteracting) {
            localStartMs = settings.startMs
            localEndMs = actualEndMs
            localTrackerPos = currentPosition
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = FocusDesign.paddingMedium)) {
        // Отображение времени (берем из локального стейта для мгновенной реакции)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(localTrackerPos), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text(formatTime(settings.durationMs), color = AppleGray, style = MaterialTheme.typography.labelSmall)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(vertical = 12.dp)
        ) {
            // Подложка
            Box(Modifier.fillMaxWidth().height(32.dp).align(Alignment.Center).background(Color.White.copy(alpha = 0.1f), CircleShape))

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = constraints.maxWidth.toFloat()

                // Хелперы для пересчета
                fun msToPx(ms: Long) = (ms.toFloat() / duration.toFloat()) * width
                fun pxToMs(px: Float) = (px / width * duration.toFloat()).toLong()

                val startPx = msToPx(localStartMs)
                val endPx = msToPx(localEndMs)
                val trackerPx = msToPx(localTrackerPos)

                // 1. Синяя зона обрезки
                Box(
                    Modifier
                        .offset { IntOffset(startPx.toInt(), 0) }
                        .width(((endPx - startPx) / density).dp)
                        .fillMaxHeight()
                        .background(iOSBlue.copy(0.2f))
                        .border(2.dp, iOSBlue, RoundedCornerShape(4.dp))
                )

                // 2. СИНИЙ ТРЕКЕР (Текущее время)
                Box(
                    Modifier
                        .offset { IntOffset(trackerPx.toInt() - 2, 0) }
                        .size(4.dp, 40.dp)
                        .background(iOSBlue, CircleShape)
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { isInteracting = true },
                                onDragEnd = { isInteracting = false },
                                onDragCancel = { isInteracting = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    // Используем позицию относительно всей ширины шкалы
                                    val newPos = pxToMs(change.position.x).coerceIn(localStartMs, localEndMs)
                                    localTrackerPos = newPos
                                    onSeek(newPos) // Плеер просто показывает кадр
                                }
                            )
                        }
                )

                // 3. ЛЕВАЯ РУЧКА (Начало)
                Box(
                    Modifier
                        .offset { IntOffset(startPx.toInt() - 15, 0) }
                        .size(30.dp, 40.dp)
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { isInteracting = true },
                                onDragEnd = {
                                    isInteracting = false
                                    onRangeChange(localStartMs, localEndMs)
                                },
                                onDragCancel = { isInteracting = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val newStart = pxToMs(change.position.x).coerceIn(0, localEndMs - 500)
                                    localStartMs = newStart
                                    // Плеер показывает только начало обрезки, трекер НЕ ДВИГАЕТСЯ
                                    onSeek(newStart)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(6.dp, 24.dp).background(Color.White, CircleShape))
                }

                // 4. ПРАВАЯ РУЧКА (Конец)
                Box(
                    Modifier
                        .offset { IntOffset(endPx.toInt() - 15, 0) }
                        .size(30.dp, 40.dp)
                        .pointerInput(duration) {
                            detectDragGestures(
                                onDragStart = { isInteracting = true },
                                onDragEnd = {
                                    isInteracting = false
                                    onRangeChange(localStartMs, localEndMs)
                                },
                                onDragCancel = { isInteracting = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val newEnd = pxToMs(change.position.x).coerceIn(localStartMs + 500, duration)
                                    localEndMs = newEnd
                                    onSeek(newEnd) // Плеер показывает конец обрезки
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(6.dp, 24.dp).background(Color.White, CircleShape))
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}