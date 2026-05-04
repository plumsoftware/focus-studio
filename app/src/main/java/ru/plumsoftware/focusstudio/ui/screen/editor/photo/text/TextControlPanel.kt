package ru.plumsoftware.focusstudio.ui.screen.editor.photo.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextElement
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun TextControlPanel(
    settings: PhotoSettings,
    selectedTextId: String?,
    onUpdate: (PhotoSettings) -> Unit,
    onClose: () -> Unit
) {
    val selectedText = settings.texts.find { it.id == selectedTextId }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedText == null) {
            // Состояние: текст не выбран. Показываем кнопку добавления в стиле iOS
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = {
                        val newText = TextElement(
                            text = "Tap to edit",
                            position = Offset(400f, 400f), // Центрируем примерно
                            color = Color.White,
                            fontSize = 30f
                        )
                        onUpdate(settings.copy(texts = settings.texts + newText))
                    },
                    color = iOSBlue,
                    shape = RoundedCornerShape(FocusDesign.cornerMedium),
                    modifier = Modifier.height(FocusDesign.languageToggleSize)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FocusDesign.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(FocusDesign.paddingSmall))
                        Text(
                            "Добавить текст",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            TextEditPanel(
                selectedText = selectedText,
                onUpdate = { updatedElement ->
                    val newList = settings.texts.map {
                        if (it.id == updatedElement.id) updatedElement else it
                    }
                    onUpdate(settings.copy(texts = newList))
                },
                onClose = onClose
            )
        }
    }
}