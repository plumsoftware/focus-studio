package ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.ColorPickerRow
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeElement
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType
import ru.plumsoftware.focusstudio.ui.theme.AppleGray

@Composable
fun ShapeControlPanel(
    settings: PhotoSettings,
    selectedShapeId: String?,
    onUpdate: (PhotoSettings) -> Unit,
    onClose: () -> Unit
) {
    val selectedShape = settings.shapes.find { it.id == selectedShapeId }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedShape == null) {
            SectionTitle("Добавить фигуру")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ShapeType.entries.forEach { type ->
                    ShapeSelectItem(type) {
                        onUpdate(settings.copy(shapes = settings.shapes + ShapeElement(type = type)))
                    }
                }
            }
        } else {
            // Заголовок с кнопкой УДАЛЕНИЯ (iOS Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onUpdate(settings.copy(shapes = settings.shapes.filter { it.id != selectedShapeId }))
                    onClose()
                }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.8f))
                }

                Text("Настройка", color = Color.White, style = MaterialTheme.typography.labelMedium)

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FocusSlider("Поворот", selectedShape.rotation) {
                    val updated = selectedShape.copy(rotation = it * 1.8f)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }

                Text("Заливка", style = MaterialTheme.typography.labelSmall, color = AppleGray)
                ColorPickerRow(selectedShape.fillColor) { color ->
                    val updated = selectedShape.copy(fillColor = color)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }

                Text("Контур", style = MaterialTheme.typography.labelSmall, color = AppleGray)
                ColorPickerRow(selectedShape.strokeColor) { color ->
                    val updated = selectedShape.copy(strokeColor = color)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }
            }
        }
    }
}