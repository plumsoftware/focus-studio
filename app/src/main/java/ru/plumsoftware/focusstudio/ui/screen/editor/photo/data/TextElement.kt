package ru.plumsoftware.focusstudio.ui.screen.editor.photo.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import java.util.UUID

data class TextElement(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Tap to edit",
    val position: Offset = Offset(200f, 200f),
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val fontSize: Float = 24f,
    val color: Color = Color.White,
    val textAlign: TextAlign = TextAlign.Center,
    val fontFamily: String = "sf_pro_regular"
)