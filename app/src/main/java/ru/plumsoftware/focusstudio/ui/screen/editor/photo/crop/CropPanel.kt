package ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.SectionTitle
import ru.plumsoftware.focusstudio.ui.theme.GradientAccent

/**
 * Панель кадрирования: пресеты пропорций и кнопка «Применить».
 * Кнопка стоит здесь, а не поверх фото — там она закрывала нижний край рамки.
 *
 * @param onRatioSelected выбран пресет (null — свободные пропорции)
 * @param onApply зафиксировать текущую рамку
 */
@Composable
fun CropPanel(
    onRatioSelected: (Float?) -> Unit,
    onApply: () -> Unit
) {
    Column {
        SectionTitle(stringResource(R.string.label_resolution))
        AspectRatioRow(onRatioSelected = onRatioSelected)

        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GradientAccent)
                    .clickable(onClick = onApply)
            ) {
                Text(
                    stringResource(R.string.btn_crop_apply).uppercase(),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}