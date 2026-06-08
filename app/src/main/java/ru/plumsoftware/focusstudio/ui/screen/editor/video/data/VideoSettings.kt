package ru.plumsoftware.focusstudio.ui.screen.editor.video.data

import android.net.Uri
import androidx.compose.ui.graphics.ColorMatrix
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeElement
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextElement

data class VideoSettings(
    val startMs: Long = 0L,
    val endMs: Long = 0L, // 0 значит конец видео не ограничен
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,

    val selectedFilter: ColorMatrix? = null,
    val filterName: String = "None",
    val clips: List<VideoClip> = emptyList(),

    val audioUri: Uri? = null,
    val audioVolume: Float = 0.5f,
    val audioFileName: String? = null,

    val shapes: List<ShapeElement> = emptyList(),
    val texts: List<TextElement> = emptyList(),

    val playbackSpeed: Float = 1f
)