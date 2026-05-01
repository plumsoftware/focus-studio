package ru.plumsoftware.focusstudio.ui.screen.photoeditor

import androidx.compose.ui.graphics.ColorMatrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.LanguageToggle
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkBg
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.Routes
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun PhotoEditorScreen(photoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current

    // Состояние истории
    val history = remember { mutableStateListOf(PhotoSettings()) }
    var currentIndex by remember { mutableIntStateOf(0) }

    val currentSettings = history[currentIndex]

    // Получение имени файла
    val fileName = remember(photoUri) {
        photoUri?.let { uri ->
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } ?: "Unknown.png"
    }

    // Функция обновления настроек с сохранением в историю
    fun updateSettings(newSettings: PhotoSettings) {
        if (newSettings != currentSettings) {
            // Удаляем "будущие" шаги, если мы сделали undo и начали менять заново
            while (history.size > currentIndex + 1) {
                history.removeAt(history.size - 1)
            }
            history.add(newSettings)
            currentIndex++
            // Ограничиваем историю 20 шагами
            if (history.size > 20) {
                history.removeAt(0)
                currentIndex--
            }
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                fileName = fileName,
                onCancel = onCancel,
                onExport = { /* Export logic */ }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            // ХОЛСТ С ФОТО
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    colorFilter = ColorFilter.colorMatrix(getCombinedMatrix(currentSettings)),
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .padding(FocusDesign.paddingMedium)
                        .graphicsLayer {
                            // РЕАЛЬНОЕ ИСКАЖЕНИЕ (SKEW)
                            // В Compose нет прямого skewX, используем матрицу через renderEffect или упрощенно rotationY для 3D искажения
                            rotationY = currentSettings.skewX * 50f
                            clip = true
                            shape = RoundedCornerShape(FocusDesign.cornerExtraSmall)
                        }
                )

                // UNDO / REDO ПАНЕЛЬ
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = FocusDesign.paddingLarge)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            null,
                            tint = if (currentIndex > 0) Color.White else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { if (currentIndex < history.size - 1) currentIndex++ },
                        enabled = currentIndex < history.size - 1
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            null,
                            tint = if (currentIndex < history.size - 1) Color.White else Color.Gray
                        )
                    }
                }
            }

            // ПАНЕЛЬ НАСТРОЕК
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FocusDesign.bottomPanelHeight),
                color = DarkSurface,
                shape = RoundedCornerShape(
                    topStart = FocusDesign.cornerLarge,
                    topEnd = FocusDesign.cornerLarge
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(FocusDesign.paddingMedium)
                        .verticalScroll(rememberScrollState())
                ) {

                    SectionTitle(stringResource(R.string.label_settings))

                    FocusSlider(
                        stringResource(R.string.param_brightness),
                        currentSettings.brightness
                    ) {
                        updateSettings(currentSettings.copy(brightness = it))
                    }
                    FocusSlider(stringResource(R.string.param_contrast), currentSettings.contrast) {
                        // Контраст обычно 0.5..1.5, мапим слайдер -100..100
                        updateSettings(currentSettings.copy(contrast = 1f + (it / 200f)))
                    }

                    Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))

                    SectionTitle(stringResource(R.string.label_filters_grade))
                    FilterRow(photoUri) { filterMatrix, name ->
                        updateSettings(
                            currentSettings.copy(
                                selectedFilter = filterMatrix,
                                filterName = name
                            )
                        )
                    }

                    // Кнопка сброса
                    TextButton(
                        onClick = { updateSettings(PhotoSettings()) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.btn_reset_all).uppercase(), color = AppleGray)
                    }
                }
            }
        }
    }
}

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

@Composable
fun FilterRow(photoUri: Uri?, onFilterSelected: (ColorMatrix?, String) -> Unit) {
    val filters = listOf(
        Triple(stringResource(R.string.filter_none), FilterMatrices.None, "None"),
        Triple(stringResource(R.string.filter_vintage), FilterMatrices.Vintage, "Vintage"),
        Triple(stringResource(R.string.filter_noir), FilterMatrices.Noir, "Noir"),
        Triple(stringResource(R.string.filter_cinema), FilterMatrices.Cinema, "Cinema")
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
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(FocusDesign.cornerMedium))
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

@Composable
fun SectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .width(4.dp)
            .height(16.dp)
            .background(iOSBlue))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
    Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))
}