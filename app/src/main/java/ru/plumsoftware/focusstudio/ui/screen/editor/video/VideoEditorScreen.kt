package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.FilterMatrices
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorToolItem
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorTopBar
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoTools
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoEditorScreen(videoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current
    var currentFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var videoAspectRatio by remember { mutableFloatStateOf(1f) }

    // ExoPlayer setup
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            videoUri?.let { setMediaItem(MediaItem.fromUri(it)) }
            setSeekParameters(SeekParameters.CLOSEST_SYNC)

            addListener(object : androidx.media3.common.Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }
            })
            prepare()
        }
    }

    var settings by remember { mutableStateOf(VideoSettings()) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    // Вкладки
    var activeTool by remember { mutableStateOf(VideoTools.TIMELINE) }

    // Извлечение реального имени файла
    val fileName = remember(videoUri) {
        videoUri?.let { uri ->
            var name = "video.mp4"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            name
        } ?: "Unknown.mp4"
    }

    // Обновление позиции трекера во время игры
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPos = exoPlayer.currentPosition
            delay(16)
        }
    }

    LaunchedEffect(activeTool) {
        if (activeTool == VideoTools.FILTERS && videoUri != null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val timeUs = exoPlayer.currentPosition * 1000
                currentFrameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                fileName = fileName,
                onCancel = onCancel,
                onExport = { /* Логика экспорта видео */ }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // 1. ПЛЕЕР + ФИЛЬТР-ОВЕРЛЕЙ
            Box(Modifier
                .weight(1f)
                .fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(12.dp))
                        .graphicsLayer {
                            // ПРИМЕНЕНИЕ ФИЛЬТРА К ВИДЕО (Android 12+)
                            if (settings.selectedFilter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val androidMatrix =
                                    android.graphics.ColorMatrix(settings.selectedFilter!!.values)
                                renderEffect =
                                    android.graphics.RenderEffect.createColorFilterEffect(
                                        ColorMatrixColorFilter(androidMatrix)
                                    ).asComposeRenderEffect()
                            }
                        }
                ) {
                    // Используем TextureView напрямую для 100% совместимости с фильтрами
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                // Привязываем TextureView к плееру
                                exoPlayer.setVideoTextureView(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Оверлей для старых версий Android (ниже 12)
                    if (settings.selectedFilter != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.01f),
                                colorFilter = ColorFilter.colorMatrix(settings.selectedFilter!!)
                            )
                        }
                    }
                }

                // Кнопка Play/Pause
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        exoPlayer.playWhenReady = isPlaying
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // 2. ПАНЕЛЬ ИНСТРУМЕНТОВ (iOS Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column {
                    // ВКЛАДКИ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditorToolItem(
                            icon = Icons.Default.History,
                            label = "Timeline",
                            isSelected = activeTool == VideoTools.TIMELINE
                        ) { activeTool = VideoTools.TIMELINE }

                        EditorToolItem(
                            icon = Icons.Default.AutoAwesome,
                            label = "Filters",
                            isSelected = activeTool == VideoTools.FILTERS
                        ) { activeTool = VideoTools.FILTERS }
                    }

                    Box(
                        modifier = Modifier
                            .height(260.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        when (activeTool) {
                            VideoTools.TIMELINE -> {
                                Column {
                                    SectionTitle(stringResource(R.string.label_timeline))
                                    VideoTimeline(
                                        settings = settings.copy(
                                            durationMs = exoPlayer.duration.coerceAtLeast(
                                                0
                                            )
                                        ),
                                        currentPosition = currentPos,
                                        onSeek = { ms ->
                                            if (System.currentTimeMillis() - lastSeekTime > 32) {
                                                exoPlayer.seekTo(ms)
                                                lastSeekTime = System.currentTimeMillis()
                                            }
                                            currentPos = ms
                                        },
                                        onRangeChange = { s, e ->
                                            settings = settings.copy(startMs = s, endMs = e)
                                        }
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    TrimButton(
                                        onClick = {
                                            // Логика обрезки (существующая)
                                            val clipStart = settings.startMs
                                            val clipEnd =
                                                if (settings.endMs == 0L) exoPlayer.duration else settings.endMs
                                            val newDuration = clipEnd - clipStart

                                            val mediaItem = MediaItem.Builder()
                                                .setUri(videoUri)
                                                .setClippingConfiguration(
                                                    MediaItem.ClippingConfiguration.Builder()
                                                        .setStartPositionMs(clipStart)
                                                        .setEndPositionMs(clipEnd).build()
                                                )
                                                .build()

                                            exoPlayer.setMediaItem(mediaItem)
                                            exoPlayer.prepare()
                                            settings = settings.copy(
                                                durationMs = newDuration,
                                                startMs = 0L,
                                                endMs = newDuration
                                            )
                                            currentPos = 0L
                                            exoPlayer.seekTo(0L)
                                        }
                                    )
                                }
                            }

                            VideoTools.FILTERS -> {
                                Column {
                                    SectionTitle("Фильтры")
                                    VideoFilterRow(
                                        previewBitmap = currentFrameBitmap,
                                        currentFilterName = settings.filterName,
                                        onFilterSelected = { matrix, name ->
                                            settings = settings.copy(
                                                selectedFilter = matrix,
                                                filterName = name
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrimButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
        shape = RoundedCornerShape(FocusDesign.cornerMedium)
    ) {
        Text("ОБРЕЗАТЬ", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VideoFilterRow(
    previewBitmap: Bitmap?,
    currentFilterName: String,
    onFilterSelected: (ColorMatrix?, String) -> Unit
) {
    val filters = listOf(
        Triple("Нет", null, "None"),
        Triple("Нуар", FilterMatrices.Noir, "Noir"),
        Triple("Винтаж", FilterMatrices.Vintage, "Vintage"),
        Triple("Кино", FilterMatrices.Cinema, "Cinema")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(filters) { (label, matrix, name) ->
            val isSelected = currentFilterName == name

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFilterSelected(matrix, name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) iOSBlue else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = matrix?.let { ColorFilter.colorMatrix(it) }
                        )
                    } else {
                        Icon(Icons.Default.Movie, null, tint = Color.White.copy(0.2f))
                    }
                }
                Text(
                    text = label,
                    color = if (isSelected) iOSBlue else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}