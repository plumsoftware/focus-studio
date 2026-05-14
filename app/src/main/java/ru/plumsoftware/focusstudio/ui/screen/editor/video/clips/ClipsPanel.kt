package ru.plumsoftware.focusstudio.ui.screen.editor.video.clips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun ClipsPanel(
    settings: VideoSettings,
    onAddClick: () -> Unit,
    onRemoveClip: (Int) -> Unit
) {
    Column {
        SectionTitle("Клипы проекта")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(settings.clips) { index, clip ->
                Box(
                    modifier = Modifier
                        .size(110.dp, 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        "Клип ${index + 1}",
                        Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    // Кнопка удаления (Маленький красный крестик в углу)
                    if (index != 0)
                        Icon(
                            Icons.Default.Cancel,
                            null,
                            tint = Color.Red.copy(0.7f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clickable { onRemoveClip(index) }
                        )
                }
            }

            item {
                Surface(
                    onClick = onAddClick,
                    modifier = Modifier.size(70.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(0.05f)
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = iOSBlue,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}