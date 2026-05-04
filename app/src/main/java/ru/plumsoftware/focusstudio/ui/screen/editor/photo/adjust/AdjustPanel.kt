package ru.plumsoftware.focusstudio.ui.screen.editor.photo.adjust

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign

@Composable
fun AdjustPanel(settings: PhotoSettings, onUpdate: (PhotoSettings) -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        SectionTitle(stringResource(R.string.label_settings))

        // Яркость
        FocusSlider(
            label = stringResource(R.string.param_brightness),
            value = settings.brightness,
            onValueChange = { onUpdate(settings.copy(brightness = it)) }
        )

        // Контраст
        FocusSlider(
            label = stringResource(R.string.param_contrast),
            value = settings.contrast,
            onValueChange = { onUpdate(settings.copy(contrast = it)) }
        )

        // Цветовой тон (Hue)
        FocusSlider(
            label = stringResource(R.string.param_hue),
            value = settings.hue,
            onValueChange = { onUpdate(settings.copy(hue = it)) }
        )

        // Насыщенность (мапим Float 0.0..2.0 в диапазон слайдера -100..100)
        FocusSlider(
            label = stringResource(R.string.param_saturation),
            value = (settings.saturation - 1f) * 100f,
            onValueChange = { onUpdate(settings.copy(saturation = 1f + (it / 100f))) }
        )

        // Размытие
        FocusSlider(
            label = stringResource(R.string.param_blur),
            value = settings.blur,
            onValueChange = { onUpdate(settings.copy(blur = it)) }
        )

        Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))
    }
}