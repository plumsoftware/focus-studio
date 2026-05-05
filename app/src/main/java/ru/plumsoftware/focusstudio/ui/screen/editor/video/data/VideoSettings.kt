package ru.plumsoftware.focusstudio.ui.screen.editor.video.data

data class VideoSettings(
    val startMs: Long = 0L,
    val endMs: Long = 0L, // 0 значит конец видео не ограничен
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false
)