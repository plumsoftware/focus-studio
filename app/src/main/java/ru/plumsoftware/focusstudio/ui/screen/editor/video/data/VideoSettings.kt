package ru.plumsoftware.focusstudio.ui.screen.editor.video.data

import androidx.compose.ui.graphics.ColorMatrix

data class VideoSettings(
    val startMs: Long = 0L,
    val endMs: Long = 0L, // 0 значит конец видео не ограничен
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,

    val selectedFilter: ColorMatrix? = null,
    val filterName: String = "None",
    val clips: List<VideoClip> = emptyList(),
)