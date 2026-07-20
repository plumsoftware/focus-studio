package ru.plumsoftware.focusstudio.ui.screen.editor.photo.filter

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.FilterMatrices
import ru.plumsoftware.focusstudio.ui.theme.AccentBlue
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.GradientAccent

@Composable
fun FilterRow(
    photoUri: Uri?,
    selectedFilterName: String = "None",
    onFilterSelected: (ColorMatrix?, String) -> Unit
) {
    val filters = listOf(
        Triple(stringResource(R.string.filter_original), FilterMatrices.None, "None"),
        Triple(stringResource(R.string.filter_vivid), FilterMatrices.Vivid, "Vivid"),
        Triple(stringResource(R.string.filter_sepia), FilterMatrices.Sepia, "Sepia"),
        Triple(stringResource(R.string.filter_polaroid), FilterMatrices.Polaroid, "Polaroid"),
        Triple(stringResource(R.string.filter_cinema), FilterMatrices.Kodachrome, "Kodachrome"),
        Triple(stringResource(R.string.filter_dramatic), FilterMatrices.DramaticBW, "DramaticBW"),
        Triple(stringResource(R.string.filter_night), FilterMatrices.NightVision, "NightVision"),
        Triple(stringResource(R.string.filter_invert), FilterMatrices.Invert, "Invert"),
        Triple(stringResource(R.string.filter_noir), FilterMatrices.Noir, "Noir"),
        Triple(stringResource(R.string.filter_vintage), FilterMatrices.Vintage, "Vintage"),
        Triple(stringResource(R.string.filter_cold), FilterMatrices.Cold, "Cold")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingMedium),
        contentPadding = PaddingValues(horizontal = FocusDesign.paddingSmall)
    ) {
        items(filters) { (label, matrix, name) ->
            val isSelected = selectedFilterName == name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFilterSelected(matrix, name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(FocusDesign.filterItemSize)
                        .clip(RoundedCornerShape(FocusDesign.cornerMedium))
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, GradientAccent, RoundedCornerShape(FocusDesign.cornerMedium))
                            } else {
                                Modifier.border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(FocusDesign.cornerMedium))
                            }
                        )
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = matrix?.let { ColorFilter.colorMatrix(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else AppleGray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}