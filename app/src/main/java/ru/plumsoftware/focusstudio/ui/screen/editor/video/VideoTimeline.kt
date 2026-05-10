package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.DragSource
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.TransitionType
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoClip
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun VideoTimeline(
    settings: VideoSettings,
    currentPosition: Long,
    onSeek: (Long) -> Unit,
    onRangeChange: (Long, Long) -> Unit,
    onClipsChange: (List<VideoClip>) -> Unit
) {
    val totalDuration = settings.clips.sumOf { it.endMs - it.startMs }.coerceAtLeast(1L)

    key(totalDuration) {
        val actualEndMs = if (settings.endMs <= 0L) totalDuration else settings.endMs

        val localStartMs = remember { mutableLongStateOf(settings.startMs) }
        val localEndMs = remember { mutableLongStateOf(actualEndMs) }
        val localTrackerPos = remember { mutableLongStateOf(currentPosition) }

        var isDragging by remember { mutableStateOf(false) }

        LaunchedEffect(currentPosition) {
            if (!isDragging) localTrackerPos.longValue = currentPosition
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            // Таймкоды
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTimeSmart(localTrackerPos.longValue, totalDuration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text(formatTimeSmart(totalDuration, totalDuration), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }

            BoxWithConstraints(Modifier.fillMaxWidth().height(80.dp)) {
                val widthPx = constraints.maxWidth.toFloat()
                val density = LocalDensity.current

                fun msToPx(ms: Long) = (ms.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) * widthPx
                fun pxToMs(px: Float) = (px / widthPx * totalDuration).toLong().coerceIn(0, totalDuration)

                val startPx = msToPx(localStartMs.longValue)
                val endPx = msToPx(localEndMs.longValue)
                val trackerPx = msToPx(localTrackerPos.longValue)

                // ТАЙМЛАЙН КОНТЕЙНЕР
                Box(Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomCenter)) {
                    // Подложка
                    Box(Modifier.fillMaxWidth().height(32.dp).align(Alignment.Center).background(Color.White.copy(0.1f), CircleShape))

                    // РИСУЕМ СЕГМЕНТЫ КЛИПОВ
                    var accumulatedMs = 0L
                    settings.clips.forEachIndexed { index, clip ->
                        val clipDur = clip.endMs - clip.startMs
                        val clipWidth = (clipDur.toFloat() / totalDuration) * widthPx
                        val clipStartOff = (accumulatedMs.toFloat() / totalDuration) * widthPx

                        Box(
                            Modifier
                                .offset { IntOffset(clipStartOff.toInt(), 16.dp.toPx().toInt()) }
                                .width(with(density) { clipWidth.toDp() })
                                .height(32.dp)
                                .background(if (index % 2 == 0) iOSBlue.copy(0.1f) else Color.White.copy(0.05f))
                                .border(0.5.dp, Color.White.copy(0.2f))
                        )
                        accumulatedMs += clipDur
                    }

                    // СИНЯЯ ЗОНА ОБРЕЗКИ ПОВЕРХ ВСЕГО
                    val regionWidth = (endPx - startPx).coerceAtLeast(0f)
                    Box(
                        Modifier
                            .offset { IntOffset(startPx.toInt(), 16.dp.run { toPx().toInt() }) }
                            .width(with(density) { regionWidth.toDp() })
                            .height(32.dp)
                            .background(iOSBlue.copy(0.2f))
                            .border(2.dp, iOSBlue, RoundedCornerShape(4.dp))
                    )

                    // РУЧКИ
                    Handle(offsetPx = startPx) { deltaX ->
                        isDragging = true
                        val newStart = pxToMs(msToPx(localStartMs.longValue) + deltaX).coerceAtMost(localEndMs.longValue - 500)
                        localStartMs.longValue = newStart
                        localTrackerPos.longValue = newStart
                        onSeek(newStart)
                        onRangeChange(newStart, localEndMs.longValue)
                    }
                    Handle(offsetPx = endPx) { deltaX ->
                        isDragging = true
                        val newEnd = pxToMs(msToPx(localEndMs.longValue) + deltaX).coerceAtLeast(localStartMs.longValue + 500)
                        localEndMs.longValue = newEnd
                        onSeek(newEnd)
                        onRangeChange(localStartMs.longValue, newEnd)
                    }

                    // ТРЕКЕР ВРЕМЕНИ
                    Box(
                        Modifier
                            .offset { IntOffset(trackerPx.toInt() - 6, 0) }
                            .size(12.dp, 64.dp)
                            .pointerInput(totalDuration) {
                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { isDragging = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newPos = pxToMs(msToPx(localTrackerPos.longValue) + dragAmount.x)
                                        localTrackerPos.longValue = newPos
                                        onSeek(newPos)
                                    }
                                )
                            }
                    ) {
                        Box(Modifier.width(2.dp).fillMaxHeight().background(Color.White).align(Alignment.Center))
                        Box(Modifier.size(12.dp).background(Color.White, CircleShape).align(Alignment.TopCenter))
                        Box(Modifier.size(12.dp).background(Color.White, CircleShape).align(Alignment.BottomCenter))
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