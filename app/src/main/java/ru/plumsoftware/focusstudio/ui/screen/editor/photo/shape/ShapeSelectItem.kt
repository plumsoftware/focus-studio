package ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.ShapeType

@Composable
fun ShapeSelectItem(type: ShapeType, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when(type) {
                    ShapeType.SQUARE -> Icons.Default.Square
                    ShapeType.CIRCLE -> Icons.Default.Circle
                    ShapeType.TRIANGLE -> Icons.Default.ChangeHistory
                    ShapeType.STAR -> Icons.Default.Star
                    ShapeType.ARROW -> Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}