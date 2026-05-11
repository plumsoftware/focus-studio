package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ColorMatrix
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoClip
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Effect
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import java.io.File
import androidx.media3.effect.RgbFilter
import android.widget.Toast
import androidx.media3.common.audio.AudioProcessor // Для корректной типизации эффектов
import androidx.media3.transformer.ProgressHolder // Вот где прячется прогресс!
import java.util.UUID
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.concat

fun trimClipsLogic(clips: List<VideoClip>, globalStart: Long, globalEnd: Long): List<VideoClip> {
    val result = mutableListOf<VideoClip>()
    var accumulatedTime = 0L

    clips.forEach { clip ->
        val clipDuration = clip.endMs - clip.startMs
        val clipGlobalStart = accumulatedTime
        val clipGlobalEnd = accumulatedTime + clipDuration

        // Проверяем, попадает ли клип в диапазон обрезки
        if (clipGlobalEnd > globalStart && clipGlobalStart < globalEnd) {
            // Вычисляем новые локальные границы для этого конкретного файла
            val newLocalStart = if (globalStart > clipGlobalStart) {
                clip.startMs + (globalStart - clipGlobalStart)
            } else {
                clip.startMs
            }

            val newLocalEnd = if (globalEnd < clipGlobalEnd) {
                clip.startMs + (globalEnd - clipGlobalStart)
            } else {
                clip.endMs
            }

            result.add(clip.copy(startMs = newLocalStart, endMs = newLocalEnd))
        }
        accumulatedTime += clipDuration
    }
    return result
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

@UnstableApi
fun exportVideo(
    context: Context,
    settings: VideoSettings,
    onProgress: (Float) -> Unit,
    onResult: (Uri?) -> Unit
) {
    val outputFileName = "FocusStudio_Export_${System.currentTimeMillis()}.mp4"
    val outputFile = File(context.cacheDir, outputFileName)

    // 1. ПОДГОТОВКА СПИСКА КЛИПОВ
    val editedMediaItems = settings.clips.map { clip ->
        val mediaItem = MediaItem.Builder()
            .setUri(clip.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build()
            )
            .build()

        // Собираем эффекты
        val videoEffects = mutableListOf<androidx.media3.common.Effect>()
        settings.selectedFilter?.let { matrix ->
            // Используем наш новый класс вместо ColorMatrix
            videoEffects.add(FocusVideoFilter(matrix))
        }

        // Прямой конструктор Effects (публичный в 1.3.1)
        val effects = androidx.media3.transformer.Effects(
            ImmutableList.of(), // audio
            ImmutableList.copyOf(videoEffects) // video
        )

        EditedMediaItem.Builder(mediaItem)
            .setEffects(effects)
            .setRemoveAudio(false)
            .build()
    }

    // 2. СОЗДАНИЕ ПОСЛЕДОВАТЕЛЬНОСТИ (Используем ПРЯМОЙ КОНСТРУКТОР)
    // В 1.3.1 конструктор EditedMediaItemSequence(List) доступен.
    val videoSequence = EditedMediaItemSequence(editedMediaItems)

    val sequences = mutableListOf<EditedMediaItemSequence>()
    sequences.add(videoSequence)

    // 3. ДОБАВЛЕНИЕ МУЗЫКИ
    if (settings.audioUri != null) {
        val totalDurationMs = settings.clips.sumOf { it.endMs - it.startMs }
        val audioMediaItem = MediaItem.Builder()
            .setUri(settings.audioUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(totalDurationMs)
                    .build()
            ).build()

        val editedAudio = EditedMediaItem.Builder(audioMediaItem).build()
        sequences.add(EditedMediaItemSequence(listOf(editedAudio)))
    }

    // 4. СБОРКА КОМПОЗИЦИИ И ТРАНСФОРМЕРА
    val composition = Composition.Builder(sequences).build()

    val transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .build()

    transformer.addListener(object : Transformer.Listener {
        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            val finalUri = saveVideoToGallery(context, outputFile, outputFileName)
            onResult(finalUri)
        }

        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            exportException.printStackTrace()
            onResult(null)
        }
    })

    // ЗАПУСК
    try {
        transformer.start(composition, outputFile.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(null)
    }

    // 5. МОНИТОРИНГ ПРОГРЕССА
    CoroutineScope(Dispatchers.Main + Job()).launch {
        val progressHolder = ProgressHolder()
        while (isActive) {
            val state = transformer.getProgress(progressHolder)
            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                onProgress(progressHolder.progress / 100f)
            } else if (state == Transformer.PROGRESS_STATE_NOT_STARTED) {
                break
            }
            delay(500)
        }
    }
}

// Хелпер сохранения (убедись, что он в проекте)
private fun saveVideoToGallery(context: Context, file: File, fileName: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/FocusStudio")
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        }
    }
    return uri
}

// Версия для Видео
fun getCombinedMatrixVideo(settings: VideoSettings): ColorMatrix {
    val result = ColorMatrix()

    settings.selectedFilter?.let { result.concat(it) }

    // В видео нет насыщенности как отдельного параметра в твоей модели,
    // но если добавишь - используй тут. Пока ставим 1f (без изменений)
    val satMatrix = ColorMatrix().apply { setToSaturation(1f) }
    result.concat(satMatrix)

    // Если в VideoSettings нет brightness/contrast, убери эти строки или добавь поля в модель
    // Предполагаем, что ты их добавил по аналогии с фото
    val contrastScale = 1f // + (settings.contrast / 200f)
    val b = 0f // settings.brightness

    val contrastMatrix = ColorMatrix(floatArrayOf(
        contrastScale, 0f, 0f, 0f, b,
        0f, contrastScale, 0f, 0f, b,
        0f, 0f, contrastScale, 0f, b,
        0f, 0f, 0f, 1f, 0f
    ))
    result.concat(contrastMatrix)

    return result
}

@androidx.media3.common.util.UnstableApi
class FocusVideoFilter(private val composeMatrix: androidx.compose.ui.graphics.ColorMatrix) : androidx.media3.effect.RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val v = composeMatrix.values

        // ТРАНСПОНИРОВАНИЕ: переводим Row-Major (Compose) в Column-Major (OpenGL/Media3)
        // Мы игнорируем 5-ю колонку (v[4], v[9], v[14], v[19]), так как 4x4 матрица
        // в OpenGL не поддерживает смещения (Offsets/Brightness) напрямую.
        // Но цветовые фильтры (Нуар, Винтаж и т.д.) теперь будут работать идеально.

        return floatArrayOf(
            v[0],  v[5],  v[10], v[15], // Колонка 1 (были начала строк)
            v[1],  v[6],  v[11], v[16], // Колонка 2
            v[2],  v[7],  v[12], v[17], // Колонка 3
            v[3],  v[8],  v[13], v[18]  // Колонка 4
        )
    }
}