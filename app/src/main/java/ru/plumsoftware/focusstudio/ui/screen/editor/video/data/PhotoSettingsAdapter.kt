package ru.plumsoftware.focusstudio.ui.screen.editor.video.data

import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings

object PhotoSettingsAdapter {
    fun toPhoto(video: VideoSettings): PhotoSettings {
        return PhotoSettings(
            texts = video.texts,
            shapes = video.shapes
        )
    }

    fun toVideo(photo: PhotoSettings, currentVideo: VideoSettings): VideoSettings {
        return currentVideo.copy(
            texts = photo.texts,
            shapes = photo.shapes
        )
    }
}