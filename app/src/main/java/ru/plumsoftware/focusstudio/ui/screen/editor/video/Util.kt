package ru.plumsoftware.focusstudio.ui.screen.editor.video

import android.annotation.SuppressLint
import androidx.media3.exoplayer.ExoPlayer
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoClip

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

fun seekToGlobalTime(exoPlayer: ExoPlayer, clips: List<VideoClip>, globalMs: Long) {
    var accumulatedMs = 0L
    for (index in clips.indices) {
        val clipDuration = clips[index].endMs - clips[index].startMs
        // Если искомое время попадает в диапазон этого клипа
        if (globalMs <= accumulatedMs + clipDuration) {
            val localMs = (globalMs - accumulatedMs) + clips[index].startMs
            exoPlayer.seekTo(index, localMs)
            return
        }
        accumulatedMs += clipDuration
    }
    // Если время за пределами (на самом конце)
    if (clips.isNotEmpty()) {
        exoPlayer.seekTo(clips.size - 1, clips.last().endMs)
    }
}