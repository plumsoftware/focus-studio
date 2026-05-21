package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.withSave
import androidx.media3.common.audio.AudioProcessor // Для корректной типизации эффектов
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.ProgressHolder // Вот где прячется прогресс!
import java.util.UUID
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.concat
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.createAndroidShapePath
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.getAndroidTypeface
import java.io.FileOutputStream

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

@androidx.media3.common.util.UnstableApi
fun exportVideo(
    context: Context,
    settings: VideoSettings,
    density: Float,
    displaySize: androidx.compose.ui.unit.IntSize,
    onResult: (Uri?) -> Unit
) {

    val outputFileName = "Focus_Export_${System.currentTimeMillis()}.mp4"
    val outputFile = java.io.File(context.cacheDir, outputFileName)

    val retriever = MediaMetadataRetriever()
    val vW: Int
    val vH: Int



    try {
        retriever.setDataSource(context, settings.clips.first().uri)
        val rotation =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()
                ?: 0
        val rawW =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
                ?: 1080
        val rawH =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
                ?: 1920

        // Определяем размеры с учетом ориентации
        val targetW = if (rotation == 90 || rotation == 270) rawH else rawW
        val targetH = if (rotation == 90 || rotation == 270) rawW else rawH

        // Ограничиваем максимальное разрешение до 720p для стабильности кодеков на слабых GPU
        val maxDimension = 720
        val scaleFactor = if (targetW > maxDimension || targetH > maxDimension) {
            maxDimension.toFloat() / maxOf(targetW, targetH).toFloat()
        } else {
            1f
        }

        // Выравниваем стороны кадра строго кратно 16 (критично для чипов Unisoc/MediaTek)
        vW = (((targetW * scaleFactor).toInt() / 16) * 16).coerceAtLeast(16)
        vH = (((targetH * scaleFactor).toInt() / 16) * 16).coerceAtLeast(16)

    } catch (e: Exception) {
        onResult(null)
        return
    } finally {
        retriever.release()
    }

    val overlayBitmap = Bitmap.createBitmap(vW, vH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(overlayBitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    overlayBitmap.eraseColor(android.graphics.Color.TRANSPARENT)

    val safeDisplayWidth = if (displaySize.width <= 0) vW else displaySize.width
    val safeDisplayHeight = if (displaySize.height <= 0) vH else displaySize.height

    val scale = vW.toFloat() / displaySize.width.toFloat()

    // РИСУЕМ ФИГУРЫ
    settings.shapes.forEach { shape ->
        val scaledWidth = shape.size.width * density * scale
        val scaledHeight = shape.size.height * density * scale
        val posX = shape.position.x * scale
        val posY = shape.position.y * scale

        canvas.save()
        canvas.translate(posX, posY)
        canvas.rotate(shape.rotation, scaledWidth / 2f, scaledHeight / 2f)

        val path = createAndroidShapePath(shape.type, scaledWidth, scaledHeight)

        if (shape.fillColor != androidx.compose.ui.graphics.Color.Transparent) {
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = shape.fillColor.toArgb()
            canvas.drawPath(path, paint)
        }
        if (shape.strokeColor != androidx.compose.ui.graphics.Color.Transparent) {
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = shape.strokeColor.toArgb()
            paint.strokeWidth = 2f * density * scale
            canvas.drawPath(path, paint)
        }
        canvas.restore()
    }

    // РИСУЕМ ТЕКСТ
    settings.texts.forEach { text ->
        val textPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = text.color.toArgb()
            textSize = text.fontSize * density * scale
            typeface = getAndroidTypeface(context, text.fontFamily)
        }

        val tx = text.position.x * scale
        val ty = text.position.y * scale

        canvas.save()
        canvas.translate(tx, ty)

        val maxWidth = (vW - tx).toInt().coerceAtLeast(1)
        val staticLayout = android.text.StaticLayout.Builder
            .obtain(text.text, 0, text.text.length, textPaint, maxWidth)
            .setIncludePad(false)
            .build()

        staticLayout.draw(canvas)
        canvas.restore()
    }

    // 3. ПОДГОТОВКА ЭФФЕКТОВ
    val videoEffects = mutableListOf<androidx.media3.common.Effect>()

    // Сначала накладываем цветовой фильтр
    settings.selectedFilter?.let { videoEffects.add(FocusVideoFilter(it)) }

    videoEffects.add(
        androidx.media3.effect.Presentation.createForWidthAndHeight(
            vW,
            vH,
            androidx.media3.effect.Presentation.LAYOUT_SCALE_TO_FIT
        )
    )

    // Затем накладываем оверлей с фигурами и текстом
    if (settings.shapes.isNotEmpty() || settings.texts.isNotEmpty()) {
        val bitmapOverlay =
            androidx.media3.effect.BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
        videoEffects.add(
            androidx.media3.effect.OverlayEffect(
                listOf<androidx.media3.effect.TextureOverlay>(bitmapOverlay)
            )
        )
    }

    // 4. СБОРКА КЛИПОВ
    val editedItems = settings.clips.map { clip ->
        val mediaItem = MediaItem.Builder()
            .setUri(clip.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build()
            )
            .build()

        EditedMediaItem.Builder(mediaItem)
            .setEffects(
                androidx.media3.transformer.Effects(
                    emptyList(),
                    videoEffects
                )
            )
            .build()
    }

    // --- ФИЛЬТР КОДЕКОВ ---
    // Создаем селектор, который полностью отсекает проблемные кодеки "unisoc"
    val customEncoderSelector = androidx.media3.transformer.EncoderSelector { mimeType ->
        val encoders = androidx.media3.transformer.EncoderUtil.getSupportedEncoders(mimeType)
        val filtered = mutableListOf<android.media.MediaCodecInfo>()
        for (i in 0 until encoders.size) {
            val encoder = encoders[i]
            if (!encoder.name.lowercase().contains("unisoc")) {
                filtered.add(encoder)
            }
        }
        ImmutableList.copyOf(if (filtered.isNotEmpty()) filtered else encoders)
    }

    // 5. ТРАНСФОРМЕР
    val transformer = Transformer.Builder(context)
        .setEncoderFactory(ForceAospEncoderFactory(context))
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .build()

    val composition = Composition.Builder(listOf(EditedMediaItemSequence(editedItems))).build()

    transformer.addListener(object : Transformer.Listener {
        override fun onCompleted(c: Composition, r: ExportResult) {
            onResult(
                saveVideoToGallery(
                    context,
                    outputFile,
                    "Focus_Export_${System.currentTimeMillis()}.mp4"
                )
            )
        }

        override fun onError(c: Composition, r: ExportResult, e: ExportException) {
            e.printStackTrace()
            onResult(null)
        }
    })

    try {
        transformer.start(composition, outputFile.absolutePath)
    } catch (e: Exception) {
        onResult(null)
    }
}

// Хелпер сохранения (убедись, что он в проекте)
private fun saveVideoToGallery(context: Context, file: File, fileName: String): Uri? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (включая EMUI 13)
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/FocusStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1) // Блокируем файл на время записи
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null

            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }

            // Снимаем флаг pending — файл теперь виден в галерее
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            // Android 9 и ниже (EMUI 9) — старый способ через File + MediaScanner
            val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_MOVIES
            )
            val appDir = File(moviesDir, "FocusStudio").apply { mkdirs() }
            val destFile = File(appDir, fileName)

            file.inputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // КРИТИЧНО для EMUI 9: без MediaScanner файл не появится в галерее!
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )

            Uri.fromFile(destFile)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
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

    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrastScale, 0f, 0f, 0f, b,
            0f, contrastScale, 0f, 0f, b,
            0f, 0f, contrastScale, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )
    )
    result.concat(contrastMatrix)

    return result
}

@androidx.media3.common.util.UnstableApi
class FocusVideoFilter(private val composeMatrix: androidx.compose.ui.graphics.ColorMatrix) :
    androidx.media3.effect.RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val v = composeMatrix.values

        // ТРАНСПОНИРОВАНИЕ: переводим Row-Major (Compose) в Column-Major (OpenGL/Media3)
        // Мы игнорируем 5-ю колонку (v[4], v[9], v[14], v[19]), так как 4x4 матрица
        // в OpenGL не поддерживает смещения (Offsets/Brightness) напрямую.
        // Но цветовые фильтры (Нуар, Винтаж и т.д.) теперь будут работать идеально.

        return floatArrayOf(
            v[0], v[5], v[10], v[15], // Колонка 1 (были начала строк)
            v[1], v[6], v[11], v[16], // Колонка 2
            v[2], v[7], v[12], v[17], // Колонка 3
            v[3], v[8], v[13], v[18]  // Колонка 4
        )
    }
}

@androidx.media3.common.util.UnstableApi
class ForceAospEncoderFactory(private val context: Context) :
    androidx.media3.transformer.Codec.EncoderFactory {

    private val delegate = androidx.media3.transformer.DefaultEncoderFactory.Builder(context)
        .setEnableFallback(false) // Отключаем fallback — он возвращает к unisoc
        .build()

    override fun createForAudioEncoding(
        format: androidx.media3.common.Format
    ): androidx.media3.transformer.Codec {
        return delegate.createForAudioEncoding(format)
    }

    override fun createForVideoEncoding(
        format: androidx.media3.common.Format
    ): androidx.media3.transformer.Codec {
        // Находим ТОЛЬКО софтварный AOSP энкодер, игнорируем unisoc/hardware
        val mimeType = format.sampleMimeType ?: MimeTypes.VIDEO_H264
        val encoders = androidx.media3.transformer.EncoderUtil.getSupportedEncoders(mimeType)

        // Приоритет: сначала c2.android (AOSP soft), потом OMX.google, потом всё кроме unisoc
        val preferred = encoders.firstOrNull {
            it.name.startsWith("c2.android")
        } ?: encoders.firstOrNull {
            it.name.startsWith("OMX.google")
        } ?: encoders.firstOrNull {
            !it.name.lowercase().contains("unisoc") &&
                    !it.name.lowercase().contains("sprd")
        }

        // Если нашли подходящий — форсируем его через модифицированный format
        val safeFormat = if (preferred != null) {
            // Понижаем разрешение если нужно для совместимости с софт-энкодером
            val maxDim = 720 // Софт-энкодер стабилен до 720p
            if (format.width > maxDim || format.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(format.width, format.height)
                format.buildUpon()
                    .setWidth(((format.width * scale).toInt() / 2) * 2)
                    .setHeight(((format.height * scale).toInt() / 2) * 2)
                    .build()
            } else format
        } else format

        return delegate.createForVideoEncoding(safeFormat)
    }

    override fun audioNeedsEncoding(): Boolean = delegate.audioNeedsEncoding()
    override fun videoNeedsEncoding(): Boolean = delegate.videoNeedsEncoding()
}