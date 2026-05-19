package ru.plumsoftware.focusstudio.ui.screen.editor.video.screen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ColorFilter
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.TextureView
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlinx.coroutines.delay
import ru.plumsoftware.focusstudio.data.AdsConfig
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.dialog.IosExportDialog
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.getFontFamily
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorToolItem
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.EditorTopBar
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape.ShapeComponent
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape.ShapeControlPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.text.TextControlPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.video.clips.ClipsPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.PhotoSettingsAdapter
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoClip
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoTools
import ru.plumsoftware.focusstudio.ui.screen.editor.video.exportVideo
import ru.plumsoftware.focusstudio.ui.screen.editor.video.filter.VideoFilterRow
import ru.plumsoftware.focusstudio.ui.screen.editor.video.getCombinedMatrixVideo
import ru.plumsoftware.focusstudio.ui.screen.editor.video.music.MusicPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.video.shape.VideoTimeline
import ru.plumsoftware.focusstudio.ui.screen.editor.video.timeline.TrimButton
import ru.plumsoftware.focusstudio.ui.screen.editor.video.trimClipsLogic
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(videoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var videoAspectRatio by remember { mutableFloatStateOf(1f) }
    var settings by remember { mutableStateOf(VideoSettings()) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(VideoTools.TIMELINE) }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var showExportDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // ID выбранных объектов
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var playerViewSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedShapeId by remember { mutableStateOf<String?>(null) }

    // --- СОСТОЯНИЕ РЕКЛАМЫ ---
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val adLoader = remember { InterstitialAdLoader(context) }

    // Загрузка рекламы при входе на экран
    LaunchedEffect(Unit) {
        val adRequest = AdRequest.Builder(AdsConfig.INTERSTITIAL_ADS_ID).build()
        adLoader.loadAd(adRequest, object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                interstitialAd = null
            }
        })
    }

    val showAdAndDialog = {
        showExportDialog = true
        if (interstitialAd != null && activity != null) {
            interstitialAd?.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(adError: AdError) {
                    // Если реклама не смогла показаться, пользователь просто увидит диалог
                }

                override fun onAdDismissed() {
                    // Реклама закрыта, под ней уже висит наш диалог
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            interstitialAd?.show(activity)
        }
    }

    // Функция обновления настроек (аналогично фото)
    fun updateSettings(newSettings: VideoSettings) {
        settings = newSettings
    }

    val exoPlayer = remember {
        val rf = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, rf).build().apply {
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(size: VideoSize) {
                    if (size.width > 0) videoAspectRatio =
                        size.width.toFloat() / size.height.toFloat()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        isPlaying = false; seekTo(0, 0); currentPos = 0L
                    }
                }
            })
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
                isPlaying = false
                exoPlayer.pause()

                exportVideo(
                    context = context,
                    displaySize = playerViewSize,
                    settings = settings.copy(selectedFilter = getCombinedMatrixVideo(settings)),
                    density = density.density,
                    onResult = { uri ->
                        isExporting = false
                        if (uri != null) {
                            showExportDialog = true
                            showAdAndDialog()
                        }

                        refreshPlaylist()
                        exoPlayer.playWhenReady = false
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
                                    RenderEffect.createColorFilterEffect(
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

                    settings.texts.forEach { textItem ->
                        val textState by rememberUpdatedState(textItem)
                        var localOffset by remember(textItem.id) { mutableStateOf(textItem.position) }
                        LaunchedEffect(textItem.position) { localOffset = textItem.position }

                        Text(
                            text = textItem.text,
                            color = textItem.color,
                            fontSize = textItem.fontSize.sp,
                            fontFamily = getFontFamily(textItem.fontFamily),
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        localOffset.x.roundToInt(),
                                        localOffset.y.roundToInt()
                                    )
                                }
                                .pointerInput(textItem.id) {
                                    detectDragGestures(
                                        onDrag = { change, drag -> change.consume(); localOffset += drag },
                                        onDragEnd = {
                                            settings = settings.copy(texts = settings.texts.map {
                                                if (it.id == textItem.id) textState.copy(position = localOffset) else it
                                            })
                                        }
                                    )
                                }
                                .clickable {
                                    selectedTextId = textItem.id; activeTool = VideoTools.TEXT
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
                    LazyRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        item {
                            EditorToolItem(
                                Icons.Default.History,
                                "Timeline",
                                activeTool == VideoTools.TIMELINE
                            ) { activeTool = VideoTools.TIMELINE }
                        }
                        item {
                            EditorToolItem(
                                Icons.Default.VideoLibrary,
                                "Clips",
                                activeTool == VideoTools.CLIPS
                            ) { activeTool = VideoTools.CLIPS }
                        }
                        item {
                            EditorToolItem(
                                Icons.Default.AutoAwesome,
                                "Filters",
                                activeTool == VideoTools.FILTERS
                            ) { activeTool = VideoTools.FILTERS }
                        }
                        item {
                            EditorToolItem(
                                Icons.Default.MusicNote,
                                "Music",
                                activeTool == VideoTools.MUSIC
                            ) { activeTool = VideoTools.MUSIC }
                        }
                        item {
                            EditorToolItem(
                                Icons.Default.TextFields,
                                "Text",
                                activeTool == VideoTools.TEXT
                            ) { activeTool = VideoTools.TEXT }
                        }
                        item {
                            EditorToolItem(
                                Icons.Default.Category,
                                "Shapes",
                                activeTool == VideoTools.SHAPES
                            ) { activeTool = VideoTools.SHAPES }
                        }
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
                                ShapeControlPanel(
                                    settings = PhotoSettingsAdapter.toPhoto(video = settings),
                                    selectedShapeId = selectedShapeId,
                                    onUpdate = {
                                        updateSettings(
                                            PhotoSettingsAdapter.toVideo(
                                                it,
                                                settings
                                            )
                                        )
                                    },
                                    onClose = {
                                        selectedShapeId = null; activeTool = VideoTools.TIMELINE
                                    }
                                )
//                                VideoShapeControlPanel(
//                                    settings = settings,
//                                    selectedShapeId = selectedShapeId,
//                                    onUpdate = { updateSettings(it) },
//                                    onClose = {
//                                        selectedShapeId = null; activeTool = VideoTools.TIMELINE
//                                    }
//                                )
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

                            VideoTools.TEXT -> {
                                TextControlPanel(
                                    settings = PhotoSettingsAdapter.toPhoto(settings),
                                    selectedTextId = selectedTextId,
                                    onUpdate = {
                                        settings = PhotoSettingsAdapter.toVideo(it, settings)
                                    },
                                    onClose = {
                                        selectedTextId = null; activeTool = VideoTools.TIMELINE
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
