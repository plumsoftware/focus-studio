package ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.calculateRectForRatio
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings

@Composable
fun CropPanel(settings: PhotoSettings, onUpdate: (PhotoSettings) -> Unit) {
    Column {
        SectionTitle(stringResource(R.string.label_resolution))
        AspectRatioRow { ratio ->
            // При клике на 3:4 мы:
            // 1. Устанавливаем ratio (чтобы рамка знала, как сохранять пропорции при тяге)
            // 2. Рассчитываем новый rect, чтобы рамка визуально изменилась сразу
            val newRect = calculateRectForRatio(ratio)
            onUpdate(
                settings.copy(
                    aspectRatio = ratio,
                    cropRect = newRect
                )
            )
        }
    }
}