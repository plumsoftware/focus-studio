package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.FilterMatrices
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.dialog.IosExportDialog
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorToolItem
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorTopBar
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape.ShapeComponent
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.TransitionType
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoClip
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoTools
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoEditorScreen(videoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current
    var videoAspectRatio by remember { mutableFloatStateOf(1f) }
    var settings by remember { mutableStateOf(VideoSettings()) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(VideoTools.TIMELINE) }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var showExportDialog by remember { mutableStateOf(false) }

    var selectedShapeId by remember { mutableStateOf<String?>(null) }
    var playerViewSize by remember { mutableStateOf(IntSize.Zero) }

    // Функция обновления настроек (аналогично фото)
    fun updateSettings(newSettings: VideoSettings) {
        settings = newSettings
    }

    val exoPlayer = remember {
        // 1. Создаем фабрику рендереров с поддержкой программного декодинга
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true) // ОЧЕНЬ ВАЖНО: разрешает переход на софт, если хард упал

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            // Устанавливаем параметры поиска кадра
            setSeekParameters(SeekParameters.CLOSEST_SYNC)

            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0) videoAspectRatio =
                        videoSize.width.toFloat() / videoSize.height.toFloat()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        playWhenReady = false
                        seekTo(0, 0)
                        currentPos = 0L
                    }
                }

                // ЛОВИМ ОШИБКИ ДЕКОДЕРА
                override fun onPlayerError(error: PlaybackException) {
                    if (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED) {
                        // Если декодер сдох, пробуем переподготовить плеер
                        prepare()
                        play()
                    }
                }
            })
            prepare()
        }
    }

    var currentFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }

    // Извлечение реального имени файла
    val fileName = remember(videoUri) {
        videoUri?.let { uri ->
            var name = "video.mp4"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) name = cursor.getString(nameIndex)
            }
            name
        } ?: "Project.mp4"
    }

    // ФУНКЦИЯ ОБНОВЛЕНИЯ ПЛЕЕРА И ГРАНИЦ (Вызывается при любом изменении клипов)
    fun applyClipsChange(newClips: List<VideoClip>) {
        val totalMs = newClips.sumOf { it.endMs - it.startMs }.coerceAtLeast(0L)

        exoPlayer.stop()

        settings = settings.copy(
            clips = newClips,
            durationMs = totalMs,
            startMs = 0L,
            endMs = totalMs
        )

        exoPlayer.clearMediaItems()
        newClips.forEach { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                ).build()
            exoPlayer.addMediaItem(mediaItem)
        }

        exoPlayer.prepare() // Заново готовим
        exoPlayer.seekTo(0L)
        currentPos = 0L
    }

    // Хелпер для поиска времени по всем клипам (чтобы не прыгало ко второму клипу)
    fun globalSeek(ms: Long) {
        var accumulated = 0L
        for (i in settings.clips.indices) {
            val clipDuration = settings.clips[i].endMs - settings.clips[i].startMs
            if (ms <= accumulated + clipDuration) {
                exoPlayer.seekTo(i, (ms - accumulated) + settings.clips[i].startMs)
                return
            }
            accumulated += clipDuration
        }
    }

    fun refreshPlaylist() {
        val currentMs = exoPlayer.currentPosition
        val currentIndex = exoPlayer.currentMediaItemIndex

        exoPlayer.clearMediaItems()
        settings.clips.forEach { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(if (clip.endMs <= 0) clip.durationMs else clip.endMs)
                        .build()
                ).build()
            exoPlayer.addMediaItem(mediaItem)
        }
        exoPlayer.prepare()
        // Возвращаем плеер на место после обновления
        if (settings.clips.isNotEmpty()) exoPlayer.seekTo(currentIndex, currentMs)
    }

    // Инициализация первым видео
    LaunchedEffect(videoUri) {
        if (videoUri != null && settings.clips.isEmpty()) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    ?: 0L
            retriever.release()

            val firstClip = VideoClip(uri = videoUri, durationMs = duration, endMs = duration)
            applyClipsChange(listOf(firstClip))
        }
    }

    // Обновляем превью при переключении на вкладку фильтров или при смене позиции в видео
    LaunchedEffect(activeTool, currentPos) {
        // Делаем это только если активна вкладка фильтров, чтобы не нагружать процессор зря
        if (activeTool == VideoTools.FILTERS && settings.clips.isNotEmpty()) {
            val retriever = MediaMetadataRetriever()
            try {
                // 1. Получаем индекс текущего клипа в плеере
                val currentIndex = exoPlayer.currentMediaItemIndex
                if (currentIndex < settings.clips.size) {
                    val currentClipUri = settings.clips[currentIndex].uri

                    // 2. Настраиваем ретривер на этот файл
                    retriever.setDataSource(context, currentClipUri)

                    // 3. Берем кадр из ТЕКУЩЕЙ позиции плеера (время в микросекундах)
                    // Используем OPTION_CLOSEST_SYNC для точности
                    val timeUs = exoPlayer.currentPosition * 1000
                    val bitmap =
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                    if (bitmap != null) {
                        currentFrameBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    // Добавление нового клипа (склейка)
    val addClipLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { newUri ->
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, newUri)
                val duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLong() ?: 0L
                retriever.release()
                settings = settings.copy(
                    clips = settings.clips + VideoClip(
                        uri = newUri,
                        durationMs = duration,
                        endMs = duration
                    )
                )
                refreshPlaylist()
            }
        }

    // Лаунчер для выбора музыки
    val musicPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val name =
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1 && cursor.moveToFirst()) cursor.getString(idx) else null
                    } ?: "music.mp3"

                settings = settings.copy(audioUri = it, audioFileName = name)
                audioPlayer.setMediaItem(MediaItem.fromUri(it))
                audioPlayer.prepare()
                audioPlayer.volume = settings.audioVolume
            }
        }

    // Позиция трекера
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            var totalPos = 0L
            val idx = exoPlayer.currentMediaItemIndex
            for (i in 0 until idx) {
                totalPos += (settings.clips[i].endMs - settings.clips[i].startMs)
            }
            currentPos = totalPos + exoPlayer.currentPosition
            delay(16)
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        if (settings.audioUri != null) {
            audioPlayer.playWhenReady = isPlaying
        }
    }

    fun syncSeek(ms: Long) {
        globalSeek(ms)
        if (settings.audioUri != null) {
            // Если музыка короче видео, используем остаток от деления
            val audioDuration = audioPlayer.duration.coerceAtLeast(1)
            audioPlayer.seekTo(ms % audioDuration)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            audioPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(fileName = fileName, onCancel = onCancel, onExport = {
                isExporting = true
                exoPlayer.stop()
                exportVideo(
                    context = context,
                    displaySize = playerViewSize,
                    settings = settings.copy(selectedFilter = getCombinedMatrixVideo(settings)),
                    onProgress = { exportProgress = it },
                    onResult = { uri ->
                        isExporting = false
                        if (uri != null) showExportDialog = true
                    }
                )
            })
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // 1. ПЛЕЕР
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = true)
                        .onGloballyPositioned { playerViewSize = it.size }
                        .clip(RoundedCornerShape(12.dp))
                        .graphicsLayer {
                            // ПРИМЕНЕНИЕ ФИЛЬТРА (API 31+)
                            if (settings.selectedFilter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect =
                                    android.graphics.RenderEffect.createColorFilterEffect(
                                        ColorMatrixColorFilter(android.graphics.ColorMatrix(settings.selectedFilter!!.values))
                                    ).asComposeRenderEffect()
                            } else {
                                renderEffect = null
                            }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                exoPlayer.setVideoTextureView(
                                    this
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Fallback для старых Android (Overlay)
                    if (settings.selectedFilter != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color.Black.copy(alpha = 0.01f),
                                colorFilter = ColorFilter.colorMatrix(settings.selectedFilter!!)
                            )
                        }
                    }

                    // --- ОТРИСОВКА ФИГУР ---
                    settings.shapes.forEach { shape ->
                        ShapeComponent(
                            shape = shape,
                            isSelected = selectedShapeId == shape.id,
                            onCommitTransform = { updatedShape ->
                                updateSettings(
                                    settings.copy(
                                    shapes = settings.shapes.map { if (it.id == shape.id) updatedShape else it }
                                ))
                            },
                            onClick = {
                                selectedShapeId = shape.id
                                activeTool = VideoTools.SHAPES
                            }
                        )
                    }
                }

                IconButton(
                    onClick = { isPlaying = !isPlaying; exoPlayer.playWhenReady = isPlaying },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White
                    )
                }
            }

            // 2. ИНСТРУМЕНТЫ
            Surface(
                Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditorToolItem(
                            Icons.Default.History,
                            "Timeline",
                            activeTool == VideoTools.TIMELINE
                        ) { activeTool = VideoTools.TIMELINE }
                        EditorToolItem(
                            Icons.Default.VideoLibrary,
                            "Clips",
                            activeTool == VideoTools.CLIPS
                        ) { activeTool = VideoTools.CLIPS }
                        EditorToolItem(
                            Icons.Default.AutoAwesome,
                            "Filters",
                            activeTool == VideoTools.FILTERS
                        ) { activeTool = VideoTools.FILTERS }
                        EditorToolItem(
                            Icons.Default.MusicNote,
                            "Music",
                            activeTool == VideoTools.MUSIC
                        ) { activeTool = VideoTools.MUSIC }
                        EditorToolItem(
                            Icons.Default.Category,
                            "Shapes",
                            activeTool == VideoTools.SHAPES
                        ) { activeTool = VideoTools.SHAPES }
                    }

                    Box(
                        Modifier
                            .height(220.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        when (activeTool) {
                            VideoTools.TIMELINE -> {
                                Column {
                                    SectionTitle("Timeline")
                                    VideoTimeline(
                                        settings = settings,
                                        currentPosition = currentPos,
                                        onSeek = { ms -> syncSeek(ms); currentPos = ms },
                                        onRangeChange = { s, e ->
                                            settings = settings.copy(startMs = s, endMs = e)
                                        },
                                        onClipsChange = { applyClipsChange(it) }
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    TrimButton(onClick = {
                                        val trimmed = trimClipsLogic(
                                            settings.clips,
                                            settings.startMs,
                                            settings.endMs
                                        )
                                        applyClipsChange(trimmed)
                                    })
                                }
                            }

                            VideoTools.CLIPS ->
                                ClipsPanel(
                                    settings = settings,
                                    onAddClick = { addClipLauncher.launch("video/*") },
                                    onRemoveClip = { index ->
                                        val newList =
                                            settings.clips.toMutableList().apply { removeAt(index) }
                                        applyClipsChange(newList)
                                    }
                                )

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

                            VideoTools.SHAPES -> {
                                VideoShapeControlPanel(
                                    settings = settings,
                                    selectedShapeId = selectedShapeId,
                                    onUpdate = { updateSettings(it) },
                                    onClose = {
                                        selectedShapeId = null; activeTool = VideoTools.TIMELINE
                                    }
                                )
                            }

                            VideoTools.MUSIC -> {
                                MusicPanel(
                                    settings = settings,
                                    onAddMusic = { musicPickerLauncher.launch("audio/*") },
                                    onVolumeChange = { vol ->
                                        settings = settings.copy(audioVolume = vol)
                                        audioPlayer.volume = vol
                                    },
                                    onRemoveMusic = {
                                        settings =
                                            settings.copy(audioUri = null, audioFileName = null)
                                        audioPlayer.stop()
                                        audioPlayer.clearMediaItems()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isExporting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = iOSBlue, strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Экспорт видео...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (showExportDialog) {
            IosExportDialog(
                onDismiss = { showExportDialog = false },
                onGoToGallery = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = "video/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )
        }
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
                // ИСПРАВЛЕНО: Теперь нажимается ВСЁ, включая "Нет" (null)
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
                            // Применяем фильтр к превью
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

@Composable
fun ClipsPanel(
    settings: VideoSettings,
    onAddClick: () -> Unit,
    onRemoveClip: (Int) -> Unit
) {
    Column {
        SectionTitle("Клипы проекта")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(settings.clips) { index, clip ->
                Box(
                    modifier = Modifier
                        .size(110.dp, 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        "Клип ${index + 1}",
                        Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    // Кнопка удаления (Маленький красный крестик в углу)
                    if (index != 0)
                        Icon(
                            Icons.Default.Cancel,
                            null,
                            tint = Color.Red.copy(0.7f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clickable { onRemoveClip(index) }
                        )
                }
            }

            item {
                Surface(
                    onClick = onAddClick,
                    modifier = Modifier.size(70.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(0.05f)
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = iOSBlue,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}