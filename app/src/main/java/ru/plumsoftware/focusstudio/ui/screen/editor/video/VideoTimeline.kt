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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    val duration = settings.durationMs.toFloat().coerceAtLeast(1f)
    val density = LocalDensity.current.density

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = FocusDesign.paddingMedium)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text(formatTime(settings.durationMs), color = AppleGray, style = MaterialTheme.typography.labelSmall)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 12.dp)
        ) {
            // Фон линии
            Box(Modifier.fillMaxSize().background(Color.White.copy(0.1f), CircleShape))

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = constraints.maxWidth.toFloat()

                // Выбранный диапазон (Синий)
                val startPx = (settings.startMs.toFloat() / duration) * width
                val endPx = (if(settings.endMs == 0L) duration else settings.endMs.toFloat()) / duration * width

                Box(
                    Modifier
                        .offset { IntOffset(startPx.toInt(), 0) }
                        .width(((endPx - startPx) / density).dp)
                        .fillMaxHeight()
                        .background(iOSBlue.copy(0.3f))
                        .border(2.dp, iOSBlue, CircleShape)
                )

                // Трекер (Белая вертикальная палочка)
                val trackerPx = (currentPosition.toFloat() / duration) * width
                Box(
                    Modifier
                        .offset { IntOffset(trackerPx.toInt() - 2, -4) }
                        .size(4.dp, 32.dp)
                        .background(Color.White, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val newPos = (change.position.x / width * duration).toLong()
                                onSeek(newPos.coerceIn(0, settings.durationMs))
                            }
                        }
                )
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