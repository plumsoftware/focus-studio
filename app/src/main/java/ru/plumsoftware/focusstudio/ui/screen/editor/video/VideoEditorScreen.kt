package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorTopBar
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoEditorScreen(videoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current

    // ExoPlayer setup
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            videoUri?.let { setMediaItem(MediaItem.fromUri(it)) }
            prepare()
        }
    }

    var settings by remember { mutableStateOf(VideoSettings()) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Обновление позиции трекера во время игры
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPos = exoPlayer.currentPosition
            delay(16)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                fileName = "VIDEO_EDIT.MP4",
                onCancel = onCancel,
                onExport = { /* Логика экспорта видео */ }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().navigationBarsPadding()) {

            // 1. ПЛЕЕР
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize(0.9f).clip(RoundedCornerShape(12.dp))
                )

                // Кнопка Play/Pause поверх видео
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        exoPlayer.playWhenReady = isPlaying
                        if (isPlaying && exoPlayer.currentPosition >= (if(settings.endMs == 0L) settings.durationMs else settings.endMs)) {
                            exoPlayer.seekTo(settings.startMs)
                        }
                    },
                    modifier = Modifier.size(64.dp).background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(40.dp)
                    )
                }
            }

            // 2. ПАНЕЛЬ ИНСТРУМЕНТОВ
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(Modifier.padding(FocusDesign.paddingMedium)) {
                    SectionTitle(stringResource(R.string.label_timeline))

                    VideoTimeline(
                        settings = settings.copy(durationMs = exoPlayer.duration.coerceAtLeast(0)),
                        currentPosition = currentPos,
                        onSeek = {
                            currentPos = it
                            exoPlayer.seekTo(it)
                        },
                        onRangeChange = { start, end ->
                            settings = settings.copy(startMs = start, endMs = end)
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Кнопка ОБРЕЗАТЬ
                    Button(
                        onClick = {
                            // Логика "Обрезать": сдвигаем начало видео
                            val clipStart = settings.startMs
                            val clipEnd = if(settings.endMs == 0L) exoPlayer.duration else settings.endMs

                            val mediaItem = MediaItem.Builder()
                                .setUri(videoUri)
                                .setClippingConfiguration(
                                    MediaItem.ClippingConfiguration.Builder()
                                        .setStartPositionMs(clipStart)
                                        .setEndPositionMs(clipEnd)
                                        .build()
                                )
                                .build()

                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            settings = VideoSettings(durationMs = clipEnd - clipStart)
                            currentPos = 0
                        },
                        modifier = Modifier.fillMaxWidth().height(FocusDesign.languageToggleSize),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
                        shape = RoundedCornerShape(FocusDesign.cornerMedium)
                    ) {
                        Text(stringResource(R.string.btn_trim).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}