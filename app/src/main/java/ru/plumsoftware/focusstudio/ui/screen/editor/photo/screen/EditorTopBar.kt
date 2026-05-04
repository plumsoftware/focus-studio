package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
            .statusBarsPadding()
            .height(FocusDesign.topBarHeight)
            .padding(horizontal = FocusDesign.paddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Лево: Отмена
        Text(
            text = stringResource(R.string.btn_cancel),
            color = iOSBlue,
            modifier = Modifier.clickable { onCancel() },
            style = MaterialTheme.typography.bodyLarge
        )

        // Центр: Название
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = fileName,
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray,
                fontSize = 10.sp
            )
        }

        // Право: Экспорт
        Surface(
            color = iOSBlue,
            shape = CircleShape,
            modifier = Modifier.clickable { onExport() }
        ) {
            Text(
                text = stringResource(R.string.btn_export),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}