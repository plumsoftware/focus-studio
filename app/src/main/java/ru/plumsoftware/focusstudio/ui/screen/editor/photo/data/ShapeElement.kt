package ru.plumsoftware.focusstudio.ui.screen.editor.photo.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import java.util.UUID

data class ShapeElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ShapeType,
    val position: Offset = Offset(300f, 300f),
    val size: Size = Size(200f, 200f),
    val rotation: Float = 0f,
    val fillColor: Color = Color.White.copy(alpha = 0.5f),
    val strokeColor: Color = Color.White,
    val strokeWidth: Float = 2f
)