package ru.plumsoftware.focusstudio.ui.screen.editor.video.clips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.screen.editor.video.data.VideoSettings
import ru.plumsoftware.focusstudio.ui.theme.AccentStart
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.GradientAccent

private val speedOptions = listOf(0.25f, 0.5f, 1f, 1.5f, 2f)

@Composable
fun ClipsPanel(
    settings: VideoSettings,
    onAddClick: () -> Unit,
    onRemoveClip: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.clips_project))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(settings.clips) { index, _ ->
                Box(
                    modifier = Modifier
                        .size(110.dp, 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        stringResource(R.string.clip_number, index + 1),
                        Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    if (index != 0) {
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
            }

            item {
                Surface(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(70.dp)
                        .border(1.dp, GradientAccent, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(0.05f)
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = AccentStart,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.clip_speed),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray
                )
                Text(
                    text = stringResource(R.string.clip_speed_value, formatSpeed(settings.playbackSpeed)),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentStart,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                speedOptions.forEach { speed ->
                    val selected = settings.playbackSpeed == speed
                    Surface(
                        onClick = { onSpeedChange(speed) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) AccentStart.copy(alpha = 0.15f) else Color.White.copy(0.05f),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) AccentStart else Color.White.copy(0.1f)
                        ),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "${formatSpeed(speed)}×",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            color = if (selected) Color.White else AppleGray,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) speed.toLong().toString() else speed.toString()
