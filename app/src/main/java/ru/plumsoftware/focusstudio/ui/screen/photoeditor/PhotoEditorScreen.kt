package ru.plumsoftware.focusstudio.ui.screen.photoeditor

import androidx.compose.ui.graphics.ColorMatrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun PhotoEditorScreen(photoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current
    val history = remember { mutableStateListOf(PhotoSettings()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentSettings = history[currentIndex]

    var activeTool by remember { mutableStateOf(EditorTools.ADJUST) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }

    // Логика истории
    fun updateSettings(newSettings: PhotoSettings) {
        if (newSettings != currentSettings) {
            // Удаляем "будущее", если мы отменили действия и начали новое
            while (history.size > currentIndex + 1) history.removeAt(history.size - 1)
            history.add(newSettings)
            currentIndex++
            // Ограничение истории для экономии памяти (например, 30 шагов)
            if (history.size > 30) {
                history.removeAt(0)
                currentIndex--
            }
        }
    }

    val fileName = remember(photoUri) {
        photoUri?.let { uri ->
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } ?: "Unknown.png"
    }

    fun updateLiveSettings(newSettings: PhotoSettings) {
        history[currentIndex] = newSettings
    }

    Scaffold(
        topBar = { EditorTopBar(fileName = fileName, onCancel = onCancel, onExport = {}) },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // 1. ОБЛАСТЬ ИЗОБРАЖЕНИЯ
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // ПАНЕЛЬ UNDO / REDO (Над фото, стиль iOS Minimalism)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (currentIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.2f)))

                    IconButton(
                        onClick = { if (currentIndex < history.size - 1) currentIndex++ },
                        enabled = currentIndex < history.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (currentIndex < history.size - 1) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Контейнер фото
                Box(
                    modifier = Modifier
                        .padding(FocusDesign.paddingMedium)
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        colorFilter = ColorFilter.colorMatrix(getCombinedMatrix(currentSettings)),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val rect = currentSettings.cropRect
                                scaleX = 1f / (rect.right - rect.left)
                                scaleY = 1f / (rect.bottom - rect.top)
                                translationX = -rect.left * size.width * scaleX
                                translationY = -rect.top * size.height * scaleY
                                rotationY = currentSettings.skewX * 40f
                                // Эффект размытия
                                if (currentSettings.blur > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                        currentSettings.blur, currentSettings.blur, android.graphics.Shader.TileMode.CLAMP
                                    ).asComposeRenderEffect()
                                }
                            }
                    )

                    // Отрисовка текста (Тут используем updateLiveSettings для плавности)
                    currentSettings.texts.forEach { textItem ->
                        var offset by remember(textItem.id, currentIndex) { mutableStateOf(textItem.position) }
                        Text(
                            text = textItem.text,
                            color = textItem.color,
                            fontSize = textItem.fontSize.sp,
                            modifier = Modifier
                                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                                .pointerInput(textItem.id) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offset += dragAmount
                                            // Живое обновление позиции без создания шага в истории
                                            val updated = textItem.copy(position = offset)
                                            updateLiveSettings(currentSettings.copy(
                                                texts = currentSettings.texts.map { if(it.id == textItem.id) updated else it }
                                            ))
                                        },
                                        onDragEnd = {
                                            // Фиксируем финальную позицию в истории
                                            val finalSettings = currentSettings.copy(
                                                texts = currentSettings.texts.map {
                                                    if(it.id == textItem.id) it.copy(position = offset) else it
                                                }
                                            )
                                            updateSettings(finalSettings)
                                        }
                                    )
                                }
                                .clickable { selectedTextId = textItem.id; activeTool = EditorTools.TEXT }
                        )
                    }

                    // Оверлей кадрирования
                    if (activeTool == EditorTools.CROP) {
                        AdvancedCropOverlay(
                            currentSettings = currentSettings,
                            onCropApply = { newRect ->
                                updateSettings(currentSettings.copy(
                                    cropRect = newRect,
                                    aspectRatio = (newRect.width / newRect.height)
                                ))
                                activeTool = EditorTools.ADJUST
                            }
                        )
                    }
                }
            }

            // НИЖНЯЯ ПАНЕЛЬ (без изменений)
            Surface(modifier = Modifier.fillMaxWidth(), color = DarkSurface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        EditorToolItem(Icons.Default.Tune, "Adjust", activeTool == EditorTools.ADJUST) { activeTool = EditorTools.ADJUST }
                        EditorToolItem(Icons.Default.AutoAwesome, "Filter", activeTool == EditorTools.FILTERS) { activeTool = EditorTools.FILTERS }
                        EditorToolItem(Icons.Default.Crop, "Crop", activeTool == EditorTools.CROP) { activeTool = EditorTools.CROP }
                        EditorToolItem(Icons.Default.TextFields, "Text", activeTool == EditorTools.TEXT) { activeTool = EditorTools.TEXT }
                    }
                    Box(modifier = Modifier.height(240.dp).padding(horizontal = 16.dp)) {
                        when (activeTool) {
                            EditorTools.ADJUST -> AdjustPanel(currentSettings) { updateSettings(it) }
                            EditorTools.FILTERS -> FilterRow(photoUri) { m, n -> updateSettings(currentSettings.copy(selectedFilter = m, filterName = n)) }
                            EditorTools.CROP -> CropPanel(currentSettings) { updateLiveSettings(it) }
                            EditorTools.TEXT -> TextControlPanel(currentSettings, selectedTextId) { updateSettings(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CropPanel(settings: PhotoSettings, onUpdate: (PhotoSettings) -> Unit) {
    Column {
        SectionTitle("Разрешение")
        AspectRatioRow { ratio ->
            // При клике на 3:4 мы:
            // 1. Устанавливаем ratio (чтобы рамка знала, как сохранять пропорции при тяге)
            // 2. Рассчитываем новый rect, чтобы рамка визуально изменилась сразу
            val newRect = calculateRectForRatio(ratio)
            onUpdate(settings.copy(
                aspectRatio = ratio,
                cropRect = newRect
            ))
        }
    }
}

@Composable
fun AspectRatioRow(onRatioSelected: (Float?) -> Unit) {
    val ratios = listOf(
        "Свободно" to null,
        "1:1" to 1f,
        "3:4" to 3f / 4f,
        "4:3" to 4f / 3f,
        "16:9" to 16f / 9f,
        "9:16" to 9f / 16f
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingMedium),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ratios) { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onRatioSelected(value) }
            ) {
                Box(
                    modifier = Modifier
                        .size(FocusDesign.cropIconSize)
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Иконка-визуализация соотношения
                    Box(
                        modifier = Modifier
                            .fillMaxSize(if (value != null) if (value > 1f) 0.5f else 0.8f else 1f)
                            .aspectRatio(value ?: 1f)
                            .background(Color.White.copy(0.3f))
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingToolbar(onCropClick: () -> Unit, isCropActive: Boolean) {
    Row(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(0.7f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* Undo */ }) { Icon(Icons.Default.Undo, null, tint = Color.White) }
        IconButton(onClick = { /* Redo */ }) { Icon(Icons.Default.Redo, null, tint = Color.White) }

        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.Gray))
        Spacer(modifier = Modifier.width(8.dp))

        // КНОПКА КРОПА
        IconButton(onClick = onCropClick) {
            Icon(
                imageVector = Icons.Default.Crop,
                contentDescription = null,
                tint = if (isCropActive) iOSBlue else Color.White
            )
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
        Triple(stringResource(R.string.filter_cinema), FilterMatrices.Cinema, "Cinema"),
        Triple(stringResource(R.string.filter_warm), FilterMatrices.Warm, "Warm"),
        Triple(stringResource(R.string.filter_cold), FilterMatrices.Cold, "Cold")
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
                        .border(
                            1.dp,
                            Color.White.copy(0.1f),
                            RoundedCornerShape(FocusDesign.cornerMedium)
                        )
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
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(iOSBlue)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
    Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))
}

@Composable
fun AdvancedCropOverlay(
    currentSettings: PhotoSettings,
    onCropApply: (Rect) -> Unit
) {
    // Синхронизируем локальный rect с тем, что приходит из пресетов
    var rect by remember(currentSettings.cropRect) { mutableStateOf(currentSettings.cropRect) }
    val ratio = currentSettings.aspectRatio

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentSettings.cropRect) { // Перезапуск при смене пресета
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / w
                    val dy = dragAmount.y / h
                    val x = change.position.x / w
                    val y = change.position.y / h

                    val threshold = 0.12f // Зона захвата краев

                    rect = when {
                        // ТЯНЕМ ПРАВЫЙ КРАЙ
                        x > rect.right - threshold -> {
                            val newRight = (rect.right + dx).coerceIn(rect.left + 0.1f, 1f)
                            if (ratio != null) {
                                // Если есть пресет, меняем высоту пропорционально
                                val newHeight = (newRight - rect.left) / ratio
                                rect.copy(right = newRight, bottom = (rect.top + newHeight).coerceAtMost(1f))
                            } else {
                                rect.copy(right = newRight)
                            }
                        }
                        // ТЯНЕМ ЛЕВЫЙ КРАЙ
                        x < rect.left + threshold -> {
                            val newLeft = (rect.left + dx).coerceIn(0f, rect.right - 0.1f)
                            if (ratio != null) {
                                val newHeight = (rect.right - newLeft) / ratio
                                rect.copy(left = newLeft, bottom = (rect.top + newHeight).coerceAtMost(1f))
                            } else {
                                rect.copy(left = newLeft)
                            }
                        }
                        // ТЯНЕМ НИЖНИЙ КРАЙ
                        y > rect.bottom - threshold -> {
                            val newBottom = (rect.bottom + dy).coerceIn(rect.top + 0.1f, 1f)
                            if (ratio != null) {
                                val newWidth = (newBottom - rect.top) * ratio
                                rect.copy(bottom = newBottom, right = (rect.left + newWidth).coerceAtMost(1f))
                            } else {
                                rect.copy(bottom = newBottom)
                            }
                        }
                        // ПЕРЕМЕЩЕНИЕ ВСЕЙ РАМКИ
                        else -> {
                            val newL = (rect.left + dx).coerceIn(0f, 1f - rect.width)
                            val newT = (rect.top + dy).coerceIn(0f, 1f - rect.height)
                            Rect(newL, newT, newL + rect.width, newT + rect.height)
                        }
                    }
                }
            }
        ) {
            val r = Rect(rect.left * w, rect.top * h, rect.right * w, rect.bottom * h)

            // Затемнение фона
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRect(r)
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, Color.Black.copy(alpha = 0.7f))

            // Рамка (iOS Style)
            drawRect(Color.White, topLeft = r.topLeft, size = r.size, style = Stroke(width = 2.dp.toPx()))

            // Ручки-овалы (индикаторы интерактивности)
            val hLen = 32.dp.toPx(); val hThick = 4.dp.toPx()
            val hColor = Color.White
            // Верх, Низ, Лево, Право
            drawRoundRect(hColor, Offset(r.center.x - hLen/2, r.top - hThick/2), Size(hLen, hThick), CornerRadius(hThick))
            drawRoundRect(hColor, Offset(r.center.x - hLen/2, r.bottom - hThick/2), Size(hLen, hThick), CornerRadius(hThick))
            drawRoundRect(hColor, Offset(r.left - hThick/2, r.center.y - hLen/2), Size(hThick, hLen), CornerRadius(hThick))
            drawRoundRect(hColor, Offset(r.right - hThick/2, r.center.y - hLen/2), Size(hThick, hLen), CornerRadius(hThick))
        }

        // Кнопка подтверждения
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .clickable { onCropApply(rect) },
            color = iOSBlue,
            shape = CircleShape
        ) {
            Text(
                "КАДРИРОВАТЬ",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun TextEditPanel(
    selectedText: TextElement?,
    onUpdate: (TextElement) -> Unit,
    onClose: () -> Unit
) {
    if (selectedText == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(FocusDesign.paddingMedium)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Текст", color = Color.White, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.Close,
                null,
                tint = Color.White,
                modifier = Modifier.clickable { onClose() })
        }

        Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))

        // Изменение текста
        BasicTextField(
            value = selectedText.text,
            onValueChange = { onUpdate(selectedText.copy(text = it)) },
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        // Размер шрифта
        FocusSlider("Размер", selectedText.fontSize) { onUpdate(selectedText.copy(fontSize = it)) }

        // Цвет
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val colors =
                listOf(Color.White, Color.Black, Color.Red, Color.Yellow, iOSBlue, Color.Green)
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(FocusDesign.colorDotSize)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (selectedText.color == color) 2.dp else 0.dp,
                            Color.White,
                            CircleShape
                        )
                        .clickable { onUpdate(selectedText.copy(color = color)) }
                )
            }
        }
    }
}

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

@Composable
fun TextControlPanel(
    settings: PhotoSettings,
    selectedTextId: String?,
    onUpdate: (PhotoSettings) -> Unit
) {
    val selectedText = settings.texts.find { it.id == selectedTextId }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedText == null) {
            // Состояние: текст не выбран. Показываем кнопку добавления в стиле iOS
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = {
                        val newText = TextElement(
                            text = "Tap to edit",
                            position = Offset(400f, 400f), // Центрируем примерно
                            color = Color.White,
                            fontSize = 30f
                        )
                        onUpdate(settings.copy(texts = settings.texts + newText))
                    },
                    color = iOSBlue,
                    shape = RoundedCornerShape(FocusDesign.cornerMedium),
                    modifier = Modifier.height(FocusDesign.languageToggleSize)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FocusDesign.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(FocusDesign.paddingSmall))
                        Text(
                            "Добавить текст",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Состояние: текст выбран. Используем ваш существующий TextEditPanel
            TextEditPanel(
                selectedText = selectedText,
                onUpdate = { updatedElement ->
                    val newList = settings.texts.map {
                        if (it.id == updatedElement.id) updatedElement else it
                    }
                    onUpdate(settings.copy(texts = newList))
                },
                onClose = {
                    // Чтобы "закрыть" панель редактирования, нам нужно сбросить ID в PhotoEditorScreen
                    // Но так как selectedTextId живет в родителе, лучше оставить управление кнопкой Close там
                }
            )
        }
    }
}

@Composable
fun AdjustPanel(settings: PhotoSettings, onUpdate: (PhotoSettings) -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        SectionTitle(stringResource(R.string.label_settings))

        // Яркость
        FocusSlider(
            label = stringResource(R.string.param_brightness),
            value = settings.brightness,
            onValueChange = { onUpdate(settings.copy(brightness = it)) }
        )

        // Контраст
        FocusSlider(
            label = stringResource(R.string.param_contrast),
            value = settings.contrast,
            onValueChange = { onUpdate(settings.copy(contrast = it)) }
        )

        // Цветовой тон (Hue)
        FocusSlider(
            label = stringResource(R.string.param_hue),
            value = settings.hue,
            onValueChange = { onUpdate(settings.copy(hue = it)) }
        )

        // Насыщенность (мапим Float 0.0..2.0 в диапазон слайдера -100..100)
        FocusSlider(
            label = stringResource(R.string.param_saturation),
            value = (settings.saturation - 1f) * 100f,
            onValueChange = { onUpdate(settings.copy(saturation = 1f + (it / 100f))) }
        )

        // Размытие
        FocusSlider(
            label = stringResource(R.string.param_blur),
            value = settings.blur,
            onValueChange = { onUpdate(settings.copy(blur = it)) }
        )

        Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))
    }
}