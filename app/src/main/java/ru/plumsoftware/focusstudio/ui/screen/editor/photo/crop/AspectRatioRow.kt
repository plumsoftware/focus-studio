package ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign

@Composable
fun AspectRatioRow(onRatioSelected: (Float?) -> Unit) {
    val ratios = listOf(
        stringResource(R.string.crop_free) to null,
        stringResource(R.string.ratio_1_1) to 1f,
        stringResource(R.string.ratio_3_4) to 3f / 4f,
        stringResource(R.string.ratio_4_3) to 4f / 3f,
        stringResource(R.string.ratio_16_9) to 16f / 9f,
        stringResource(R.string.ratio_9_16) to 9f / 16f
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingMedium),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ratios) { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onRatioSelected(value) }
            ) {
                Box(
                    modifier = Modifier
                        .size(FocusDesign.cropIconSize)
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(if (value != null) if (value > 1f) 0.5f else 0.8f else 1f)
                            .aspectRatio(value ?: 1f)
                            .background(Color.White.copy(0.3f))
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
