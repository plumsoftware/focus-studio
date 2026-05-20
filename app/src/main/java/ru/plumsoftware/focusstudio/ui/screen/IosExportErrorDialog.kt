package ru.plumsoftware.focusstudio.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun IosExportErrorDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier.width(270.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(20.dp))

                Text(
                    "Ошибка экспорта",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Не удалось экспортировать видео. На устройстве возникла системная ошибка обработки видеопотока.",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                Divider(color = Color.White.copy(0.1f), thickness = 0.5.dp)

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = iOSBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}