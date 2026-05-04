package ru.plumsoftware.focusstudio.ui.screen.editor.photo.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextElement
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun TextEditPanel(
    selectedText: TextElement?,
    onUpdate: (TextElement) -> Unit,
    onClose: () -> Unit
) {
    if (selectedText == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(FocusDesign.paddingMedium)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Текст", color = Color.White, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.Close,
                null,
                tint = Color.White,
                modifier = Modifier.clickable { onClose() })
        }

        Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))

        // Изменение текста
        BasicTextField(
            value = selectedText.text,
            onValueChange = { onUpdate(selectedText.copy(text = it)) },
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        // Размер шрифта
        FocusSlider("Размер", selectedText.fontSize) { onUpdate(selectedText.copy(fontSize = it)) }

        // Цвет
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val colors =
                listOf(Color.White, Color.Black, Color.Red, Color.Yellow, iOSBlue, Color.Green)
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(FocusDesign.colorDotSize)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (selectedText.color == color) 2.dp else 0.dp,
                            Color.White,
                            CircleShape
                        )
                        .clickable { onUpdate(selectedText.copy(color = color)) }
                )
            }
        }
    }
}