package ru.plumsoftware.focusstudio.ui.screen.editor.video.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun MusicPanel(
    settings: VideoSettings,
    onAddMusic: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onRemoveMusic: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Музыка")

        if (settings.audioUri == null) {
            // Состояние: Музыка не добавлена
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onAddMusic() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = iOSBlue, modifier = Modifier.size(32.dp))
                    Text("Добавить аудио", color = iOSBlue, style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            // Состояние: Музыка добавлена
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, tint = iOSBlue)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = settings.audioFileName ?: "Аудио трек",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onRemoveMusic) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.7f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Громкость (iOS Style)
                FocusSlider(
                    label = "Громкость",
                    value = settings.audioVolume * 100f,
                    onValueChange = { onVolumeChange(it / 100f) }
                )
            }

            TextButton(
                onClick = onAddMusic,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Заменить трек", color = iOSBlue)
            }
        }
    }
}