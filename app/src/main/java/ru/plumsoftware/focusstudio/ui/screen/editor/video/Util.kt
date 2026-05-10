package ru.plumsoftware.focusstudio.ui.screen.editor.video

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