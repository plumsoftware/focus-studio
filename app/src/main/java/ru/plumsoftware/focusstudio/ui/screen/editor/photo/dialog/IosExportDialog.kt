package ru.plumsoftware.focusstudio.ui.screen.editor.photo.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun IosExportDialog(
    onDismiss: () -> Unit,
    onGoToGallery: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1C1C1E).copy(alpha = 0.95f), // iOS Dark Vibrant
            modifier = Modifier.width(270.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text(
                    text = "Готово!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ваше изображение успешно сохранено в галерею.",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Кнопка в стиле iOS (с тонкими разделителями)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onGoToGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Перейти в галерею",
                        color = Color(0xFF0A84FF), // iOS System Blue
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OK",
                        color = Color(0xFF0A84FF),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}