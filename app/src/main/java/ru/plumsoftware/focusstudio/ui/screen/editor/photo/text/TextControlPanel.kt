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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Divider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextBackgroundStyle
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextElement
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.getFontFamily
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.parseColor
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.FocusSlider
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.toHex
import ru.plumsoftware.focusstudio.ui.theme.AccentStart
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.GradientAccent
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue
import java.time.format.TextStyle
import java.util.UUID

private enum class TextEditTab { FONT, COLOR, BACKGROUND }

@Composable
fun TextEditPanel(
    selectedText: TextElement,
    onUpdate: (TextElement) -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf(TextEditTab.FONT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(DarkSurface)
            .verticalScroll(rememberScrollState())
            .padding(FocusDesign.paddingMedium)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.text_edit_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }

        BasicTextField(
            value = selectedText.text,
            onValueChange = { onUpdate(selectedText.copy(text = it)) },
            textStyle = androidx.compose.ui.text.TextStyle(
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

        // Ряд табов, как "Аа Шрифты | Цвет | Обводка | Тень" на промо
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextEditTabItem(stringResource(R.string.label_font), activeTab == TextEditTab.FONT) {
                activeTab = TextEditTab.FONT
            }
            TextEditTabItem(stringResource(R.string.label_color), activeTab == TextEditTab.COLOR) {
                activeTab = TextEditTab.COLOR
            }
            TextEditTabItem(stringResource(R.string.label_background), activeTab == TextEditTab.BACKGROUND) {
                activeTab = TextEditTab.BACKGROUND
            }
        }

        Divider(color = Color.White.copy(0.08f), thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))

        Column(modifier = Modifier.padding(top = 12.dp)) {
            when (activeTab) {
                TextEditTab.FONT -> FontTabContent(selectedText, onUpdate)
                TextEditTab.COLOR -> ColorTabContent(selectedText, onUpdate)
                TextEditTab.BACKGROUND -> BackgroundTabContent(selectedText, onUpdate)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun TextEditTabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(bottom = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else AppleGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(GradientAccent)
            )
        }
    }
}

@Composable
private fun FontTabContent(selectedText: TextElement, onUpdate: (TextElement) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(fontOptions) { option ->
            val isSelected = selectedText.fontFamily == option.key
            Surface(
                onClick = { onUpdate(selectedText.copy(fontFamily = option.key)) },
                color = Color.White.copy(0.1f),
                shape = RoundedCornerShape(FocusDesign.cornerMedium),
                modifier = Modifier.then(
                    if (isSelected) Modifier.border(2.dp, GradientAccent, RoundedCornerShape(FocusDesign.cornerMedium))
                    else Modifier
                )
            ) {
                Text(
                    text = stringResource(option.labelRes),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = getFontFamily(option.key),
                        fontSize = 14.sp
                    )
                )
            }
        }
    }

    FocusSlider(
        label = stringResource(R.string.param_size),
        value = selectedText.fontSize,
        valueRange = 10f..100f
    ) {
        onUpdate(selectedText.copy(fontSize = it))
    }
}

@Composable
private fun ColorTabContent(selectedText: TextElement, onUpdate: (TextElement) -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        val quickColors = listOf(
            Color.White, Color.Black, Color.Red, Color(0xFFFF5722),
            Color.Yellow, Color.Green, AccentStart, Color(0xFF9C27B0)
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
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            ),
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

// НОВОЕ: выбор фона текста — нет / сплошной / градиент (с готовыми пресетами)
@Composable
private fun BackgroundTabContent(selectedText: TextElement, onUpdate: (TextElement) -> Unit) {
    val gradientPresets = listOf(
        Color(0xFF6C5CE7) to Color(0xFFFF5A8A), // как на промо
        Color(0xFF4A6CF7) to Color(0xFF6C5CE7),
        Color(0xFFFF9A00) to Color(0xFFFF5A8A),
        Color(0xFF00C9A7) to Color(0xFF00B0FF)
    )
    val solidColors = listOf(
        Color.White, Color.Black, Color.Red, Color(0xFFFF5722),
        Color.Yellow, Color.Green, AccentStart, Color(0xFF9C27B0)
    )

    Text(
        stringResource(R.string.label_background).uppercase(),
        color = AppleGray,
        style = MaterialTheme.typography.labelSmall
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // "Нет" — прозрачный вариант, крестик как в ColorPickerRow
        item {
            val isSelected = selectedText.backgroundStyle is TextBackgroundStyle.None
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(0.2f),
                        shape = CircleShape
                    )
                    .clickable { onUpdate(selectedText.copy(backgroundStyle = TextBackgroundStyle.None)) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .graphicsLayer(rotationZ = 45f)
                        .background(Color.Red)
                )
            }
        }

        // Градиентные пресеты
        items(gradientPresets) { (start, end) ->
            val isSelected = (selectedText.backgroundStyle as? TextBackgroundStyle.Gradient)
                ?.let { it.start == start && it.end == end } ?: false
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(start, end)))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(0.2f),
                        shape = CircleShape
                    )
                    .clickable {
                        onUpdate(selectedText.copy(backgroundStyle = TextBackgroundStyle.Gradient(start, end)))
                    }
            )
        }

        // Сплошные цвета
        items(solidColors) { color ->
            val isSelected = (selectedText.backgroundStyle as? TextBackgroundStyle.Solid)?.color == color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(0.2f),
                        shape = CircleShape
                    )
                    .clickable {
                        onUpdate(selectedText.copy(backgroundStyle = TextBackgroundStyle.Solid(color)))
                    }
            )
        }
    }
}
