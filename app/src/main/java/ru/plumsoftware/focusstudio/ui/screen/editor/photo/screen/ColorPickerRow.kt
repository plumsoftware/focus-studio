package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.AccentStart

@Composable
fun ColorPickerRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Transparent, // Добавлено: Прозрачный
        Color.White, Color.Gray, Color.Black,
        Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
        Color(0xFF4CD964), Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFFAF52DE)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = FocusDesign.paddingSmall),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(FocusDesign.colorDotSize)
                    .clip(CircleShape)
                    .background(if (color == Color.Transparent) Color.White.copy(0.1f) else color)
                    .border(
                        width = if (selectedColor == color) 2.dp else 1.dp,
                        color = if (selectedColor == color) AccentStart else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (color == Color.Transparent) {
                    // Символ прозрачности (красная линия)
                    Box(modifier = Modifier.width(20.dp).height(2.dp).graphicsLayer(rotationZ = 45f).background(Color.Red))
                }
            }
        }
    }
}