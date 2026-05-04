package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun FocusSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    // Вычисляем отображение текста: если больше 0, добавляем "+", иначе просто число
    val displayValue = if (value > 0) "+${value.toInt()}" else "${value.toInt()}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FocusDesign.paddingSmall)
            .height(FocusDesign.sliderHeight) // 44.dp из FocusDesign
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(), // Если нужен капс как на скрине
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.labelSmall,
                color = iOSBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = iOSBlue,
                inactiveTrackColor = Color.DarkGray.copy(alpha = 0.3f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}