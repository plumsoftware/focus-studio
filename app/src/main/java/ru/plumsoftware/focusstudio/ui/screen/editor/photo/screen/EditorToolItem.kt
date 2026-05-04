package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun EditorToolItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(FocusDesign.cornerExtraSmall))
            .clickable { onClick() }
            .padding(vertical = FocusDesign.paddingSmall)
            .width(72.dp) // Фиксированная ширина для равномерного распределения
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            // Используем iOSBlue для активного состояния и AppleGray для неактивного
            tint = if (isSelected) iOSBlue else AppleGray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            // Используем вашу типографику SF-Pro
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) iOSBlue else AppleGray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}