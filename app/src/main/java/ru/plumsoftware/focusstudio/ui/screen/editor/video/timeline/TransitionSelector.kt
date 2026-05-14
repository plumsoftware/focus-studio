package ru.plumsoftware.focusstudio.ui.screen.editor.video.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.TransitionType
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun TransitionSelector(
    currentType: TransitionType,
    onSelect: (TransitionType) -> Unit
) {
    Column {
        SectionTitle("Выберите переход")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TransitionItem("Нет", TransitionType.NONE, currentType == TransitionType.NONE) { onSelect(TransitionType.NONE) }
            TransitionItem("Fade", TransitionType.FADE, currentType == TransitionType.FADE) { onSelect(TransitionType.FADE) }
        }
    }
}

@Composable
fun TransitionItem(label: String, type: TransitionType, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) iOSBlue else Color.White.copy(0.1f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (type == TransitionType.FADE) Icons.Default.BlurOn else Icons.Default.Close,
                null, tint = if (isSelected) Color.White else AppleGray
            )
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}