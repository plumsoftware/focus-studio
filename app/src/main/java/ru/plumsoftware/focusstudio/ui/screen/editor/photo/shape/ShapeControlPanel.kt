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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import ru.plumsoftware.focusstudio.R
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
    canvasSize: IntSize,
    onUpdate: (PhotoSettings) -> Unit,
    onShapeAdded: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val selectedShape = settings.shapes.find { it.id == selectedShapeId }
    val density = LocalDensity.current.density

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedShape == null) {
            SectionTitle(stringResource(R.string.shape_add))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ShapeType.entries.forEach { type ->
                    ShapeSelectItem(type) {
                        val newShape = newCenteredShape(type, canvasSize, density)
                        onUpdate(settings.copy(shapes = settings.shapes + newShape))
                        onShapeAdded(newShape.id)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onUpdate(settings.copy(shapes = settings.shapes.filter { it.id != selectedShapeId }))
                    onClose()
                }) {
                    Icon(
                        Icons.Default.Delete,
                        stringResource(R.string.cd_delete),
                        tint = Color.Red.copy(0.8f)
                    )
                }

                Text(
                    stringResource(R.string.shape_settings),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FocusSlider(
                    label = stringResource(R.string.param_rotation),
                    value = selectedShape.rotation
                ) {
                    val updated = selectedShape.copy(rotation = it * 1.8f)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }

                Text(
                    stringResource(R.string.label_fill),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray
                )
                ColorPickerRow(selectedShape.fillColor) { color ->
                    val updated = selectedShape.copy(fillColor = color)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }

                Text(
                    stringResource(R.string.label_stroke),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray
                )
                ColorPickerRow(selectedShape.strokeColor) { color ->
                    val updated = selectedShape.copy(strokeColor = color)
                    onUpdate(settings.copy(shapes = settings.shapes.map { s -> if (s.id == updated.id) updated else s }))
                }
            }
        }
    }
}


/**
 * Новая фигура ровно по центру области редактирования.
 * position — левый верхний угол в пикселях, size — в dp, поэтому для центрирования
 * размер переводится в пиксели. Фигура не больше 40% меньшей стороны области.
 */
private fun newCenteredShape(type: ShapeType, canvasSize: IntSize, density: Float): ShapeElement {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return ShapeElement(type = type)

    val maxSideDp = minOf(canvasSize.width, canvasSize.height) * 0.4f / density
    val sideDp = minOf(120f, maxSideDp).coerceAtLeast(30f)
    val sidePx = sideDp * density

    return ShapeElement(
        type = type,
        position = Offset(
            (canvasSize.width - sidePx) / 2f,
            (canvasSize.height - sidePx) / 2f
        ),
        size = Size(sideDp, sideDp)
    )
}