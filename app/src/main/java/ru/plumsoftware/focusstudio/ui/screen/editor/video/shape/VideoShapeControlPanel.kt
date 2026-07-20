package ru.plumsoftware.focusstudio.ui.screen.editor.video.shape

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
import androidx.compose.ui.res.stringResource
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeElement
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.ColorPickerRow
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape.ShapeSelectItem
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings

@Composable
fun VideoShapeControlPanel(
    settings: VideoSettings,
    selectedShapeId: String?,
    onUpdate: (VideoSettings) -> Unit,
    onClose: () -> Unit
) {
    val selectedShape = settings.shapes.find { it.id == selectedShapeId }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedShape == null) {
            SectionTitle(stringResource(R.string.shape_add))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ShapeType.entries.forEach { type ->
                    ShapeSelectItem(type) {
                        onUpdate(settings.copy(shapes = settings.shapes + ShapeElement(type = type)))
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onUpdate(settings.copy(shapes = settings.shapes.filter { it.id != selectedShapeId }))
                    onClose()
                }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                Text(stringResource(R.string.shape_settings_full), color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FocusSlider(
                    label = stringResource(R.string.param_rotation),
                    value = selectedShape.rotation / 1.8f
                ) {
                    val updated = selectedShape.copy(rotation = it * 1.8f)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }
                Text(
                    stringResource(R.string.label_fill),
                    color = ru.plumsoftware.focusstudio.ui.theme.AppleGray,
                    style = MaterialTheme.typography.labelSmall
                )
                ColorPickerRow(selectedShape.fillColor) { color ->
                    val updated = selectedShape.copy(fillColor = color)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }
            }
        }
    }
}
