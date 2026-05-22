package ru.plumsoftware.focusstudio.ui.screen.editor.photo.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextElement
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.getFontFamily
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.parseColor
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.toHex
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun TextEditPanel(
    selectedText: TextElement,
    onUpdate: (TextElement) -> Unit,
    onClose: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    // Состояние скролла
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(DarkSurface)
            .verticalScroll(scrollState) // ВКЛЮЧАЕМ СКРОЛЛ
            .padding(FocusDesign.paddingMedium)
    ) {
        // Шапка
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Настроить текст", color = Color.White, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }

        // 1. Поле ввода (внутри панели шрифт уже применяется)
        BasicTextField(
            value = selectedText.text,
            onValueChange = { onUpdate(selectedText.copy(text = it)) },
            textStyle = TextStyle(
                color = selectedText.color,
                fontSize = 18.sp,
                fontFamily = getFontFamily(selectedText.fontFamily)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))

        // 2. Выбор шрифта
        Text("ШРИФТ", color = AppleGray, style = MaterialTheme.typography.labelSmall)
        val fontList = listOf(
            "Default", "Serif", "Sans serif", "Monospace",
            "SF Pro", "Google Sans", "Passions Conflict",
            "Ruthless Sketch", "Montserrat Underline", "Old Soviet", "AA Stetica", "Accidental Presidency"
        )

        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fontList) { fontName ->
                val isSelected = selectedText.fontFamily == fontName
                Surface(
                    onClick = { onUpdate(selectedText.copy(fontFamily = fontName)) },
                    color = if (isSelected) iOSBlue else Color.White.copy(0.1f),
                    shape = RoundedCornerShape(FocusDesign.cornerMedium)
                ) {
                    Text(
                        text = fontName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        style = TextStyle(fontFamily = getFontFamily(fontName), fontSize = 14.sp)
                    )
                }
            }
        }

        // 3. Размер
        FocusSlider("Размер", selectedText.fontSize, valueRange = 10f..100f) {
            onUpdate(selectedText.copy(fontSize = it))
        }

        // 4. Цвета
        Text("ЦВЕТ", color = AppleGray, style = MaterialTheme.typography.labelSmall)

        // Обернул в Column, чтобы HEX-пикер не уезжал
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val quickColors = listOf(
                    Color.White, Color.Black, Color.Red, Color(0xFFFF5722),
                    Color.Yellow, Color.Green, iOSBlue, Color(0xFF9C27B0)
                )
                items(quickColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedText.color == color) 2.dp else 1.dp,
                                color = if (selectedText.color == color) Color.White else Color.White.copy(0.2f),
                                shape = CircleShape
                            )
                            .clickable { onUpdate(selectedText.copy(color = color)) }
                    )
                }
            }

            // HEX Пикер вынес чуть ниже, чтобы было удобнее нажимать
            var hexInput by remember(selectedText.color) { mutableStateOf(selectedText.color.toHex()) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = hexInput,
                    onValueChange = {
                        hexInput = it
                        it.parseColor()?.let { newColor -> onUpdate(selectedText.copy(color = newColor)) }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    singleLine = true
                )
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(selectedText.color.toHex())) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        clipboardManager.getText()?.text?.parseColor()?.let { onUpdate(selectedText.copy(color = it)) }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }

        // Дополнительный отступ снизу для комфортного скролла
        Spacer(modifier = Modifier.height(20.dp))
    }
}