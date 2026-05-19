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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.FilterMatrices
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign

@Composable
fun FilterRow(photoUri: Uri?, onFilterSelected: (ColorMatrix?, String) -> Unit) {
    val filters = listOf(
        Triple("Оригинал", FilterMatrices.None, "None"),
        Triple("Vivid", FilterMatrices.Vivid, "Vivid"),
        Triple("Sepia", FilterMatrices.Sepia, "Sepia"),
        Triple("Polaroid", FilterMatrices.Polaroid, "Polaroid"),
        Triple("Cinema", FilterMatrices.Kodachrome, "Kodachrome"),
        Triple("Dramatic", FilterMatrices.DramaticBW, "DramaticBW"),
        Triple("Night", FilterMatrices.NightVision, "NightVision"),
        Triple("Invert", FilterMatrices.Invert, "Invert"),
        Triple("Noir", FilterMatrices.Noir, "Noir"),
        Triple("Vintage", FilterMatrices.Vintage, "Vintage"),
        Triple("Cold", FilterMatrices.Cold, "Cold")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingMedium),
        contentPadding = PaddingValues(horizontal = FocusDesign.paddingSmall)
    ) {
        items(filters) { (label, matrix, name) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFilterSelected(matrix, name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(FocusDesign.filterItemSize)
                        .clip(RoundedCornerShape(FocusDesign.cornerMedium))
                        .border(
                            1.dp,
                            Color.White.copy(0.1f),
                            RoundedCornerShape(FocusDesign.cornerMedium)
                        )
                ) {
                    // ПРЕВЬЮ ФИЛЬТРА
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
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}