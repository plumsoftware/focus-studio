package ru.plumsoftware.focusstudio.ui.screen.editor.video.filter

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.FilterMatrices
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun VideoFilterRow(
    previewBitmap: Bitmap?,
    currentFilterName: String,
    onFilterSelected: (ColorMatrix?, String) -> Unit
) {
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(filters) { (label, matrix, name) ->
            val isSelected = currentFilterName == name

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // ИСПРАВЛЕНО: Теперь нажимается ВСЁ, включая "Нет" (null)
                modifier = Modifier.clickable { onFilterSelected(matrix, name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) iOSBlue else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            // Применяем фильтр к превью
                            colorFilter = matrix?.let { ColorFilter.colorMatrix(it) }
                        )
                    } else {
                        Icon(Icons.Default.Movie, null, tint = Color.White.copy(0.2f))
                    }
                }
                Text(
                    text = label,
                    color = if (isSelected) iOSBlue else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}