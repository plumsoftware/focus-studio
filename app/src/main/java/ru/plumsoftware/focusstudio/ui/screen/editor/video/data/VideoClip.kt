package ru.plumsoftware.focusstudio.ui.screen.editor.video.data

import android.net.Uri
import java.util.UUID

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val durationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val transition: TransitionType = TransitionType.NONE
)