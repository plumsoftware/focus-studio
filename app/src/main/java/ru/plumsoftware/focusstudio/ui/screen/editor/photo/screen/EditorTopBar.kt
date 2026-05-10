package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun EditorTopBar(fileName: String, onCancel: () -> Unit, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // Отступ под системный статус-бар
            .height(FocusDesign.topBarHeight) // Стандарт 64.dp
            .padding(horizontal = FocusDesign.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ЛЕВО: Отмена (Занимает только свою ширину)
        Text(
            text = stringResource(R.string.btn_cancel),
            color = iOSBlue,
            modifier = Modifier
                .wrapContentWidth()
                .clickable { onCancel() },
            style = MaterialTheme.typography.bodyLarge
        )

        // ЦЕНТР: Название файла (Занимает всё свободное пространство)
        Column(
            modifier = Modifier
                .weight(1f) // Ключевое исправление: забирает только свободное место
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = fileName,
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Если имя слишком длинное, будет "имя_файла..."
            )
        }

        // ПРАВО: Экспорт (Занимает только свою ширину)
        Surface(
            color = iOSBlue,
            shape = CircleShape,
            modifier = Modifier
                .wrapContentWidth()
                .clickable { onExport() }
        ) {
            Text(
                text = stringResource(R.string.btn_export),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}