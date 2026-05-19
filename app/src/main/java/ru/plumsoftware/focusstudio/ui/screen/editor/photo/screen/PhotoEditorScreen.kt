package ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import ru.plumsoftware.focusstudio.data.AdsConfig
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.adjust.AdjustPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop.AdvancedCropOverlay
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.EditorTools
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.crop.CropPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.dialog.IosExportDialog
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.filter.FilterRow
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.getCombinedMatrix
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.getFontFamily
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.saveEditedImage
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape.ShapeComponent
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.shape.ShapeControlPanel
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.text.TextControlPanel
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun PhotoEditorScreen(photoUri: Uri?, onCancel: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val history = remember { mutableStateListOf(PhotoSettings()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentSettings = history[currentIndex]

    var activeTool by remember { mutableStateOf(EditorTools.ADJUST) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var selectedShapeId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // --- СОСТОЯНИЕ РЕКЛАМЫ ---
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val adLoader = remember { InterstitialAdLoader(context) }

    // Загрузка рекламы при входе на экран
    LaunchedEffect(Unit) {
        val adRequest = AdRequest.Builder(AdsConfig.INTERSTITIAL_ADS_ID).build()
        adLoader.loadAd(adRequest, object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                interstitialAd = null
            }
        })
    }

    val showAdAndDialog = {
        showExportDialog = true
        if (interstitialAd != null && activity != null) {
            interstitialAd?.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(adError: com.yandex.mobile.ads.common.AdError) {
                    // Если реклама не смогла показаться, пользователь просто увидит диалог
                }

                override fun onAdDismissed() {
                    // Реклама закрыта, под ней уже висит наш диалог
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            interstitialAd?.show(activity)
        }
    }

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
        topBar = {
            EditorTopBar(fileName = fileName, onCancel = onCancel, onExport = {
                if (photoUri != null && !isExporting) {
                    isExporting = true
                    saveEditedImage(context, photoUri, currentSettings, boxSize) { uri ->
                        showExportDialog = true
                        isExporting = false
                        if (uri != null)
                            showAdAndDialog()
                    }
                }
            })
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

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

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    IconButton(
                        onClick = { if (currentIndex < history.size - 1) currentIndex++ },
                        enabled = currentIndex < history.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (currentIndex < history.size - 1) Color.White else Color.White.copy(
                                alpha = 0.3f
                            ),
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
                        .onGloballyPositioned { boxSize = it.size }
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
                                    renderEffect = RenderEffect.createBlurEffect(
                                        currentSettings.blur,
                                        currentSettings.blur,
                                        Shader.TileMode.CLAMP
                                    ).asComposeRenderEffect()
                                }
                            }
                    )

                    // Отрисовка текста
                    currentSettings.texts.forEach { textItem ->
                        // rememberUpdatedState гарантирует, что настройки (цвет, размер) не "протухнут" во время захвата жеста
                        val currentTextState by rememberUpdatedState(textItem)

                        // Локальное состояние позиции — ключ к плавности 60 FPS
                        var localOffset by remember(textItem.id) { mutableStateOf(textItem.position) }

                        // Синхронизация: если нажали Undo/Redo, обновляем локальную позицию
                        LaunchedEffect(textItem.position) {
                            localOffset = textItem.position
                        }

                        Text(
                            text = textItem.text,
                            color = textItem.color,
                            fontSize = textItem.fontSize.sp,
                            fontFamily = getFontFamily(textItem.fontFamily),
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        localOffset.x.roundToInt(),
                                        localOffset.y.roundToInt()
                                    )
                                }
                                .pointerInput(textItem.id) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            localOffset += dragAmount
                                        },
                                        onDragEnd = {
                                            val finalUpdate =
                                                currentTextState.copy(position = localOffset)
                                            updateSettings(
                                                currentSettings.copy(
                                                    texts = currentSettings.texts.map {
                                                        if (it.id == textItem.id) finalUpdate else it
                                                    }
                                                ))
                                        }
                                    )
                                }
                                .clickable {
                                    selectedTextId = textItem.id
                                    activeTool = EditorTools.TEXT
                                }
                                .padding(4.dp)
                        )
                    }

                    currentSettings.shapes.forEach { shape ->
                        ShapeComponent(
                            shape = shape,
                            isSelected = selectedShapeId == shape.id,
                            onCommitTransform = { updated ->
                                updateSettings(
                                    currentSettings.copy(
                                        shapes = currentSettings.shapes.map { if (it.id == shape.id) updated else it }
                                    ))
                            },
                            onClick = {
                                selectedShapeId = shape.id
                                activeTool = EditorTools.SHAPES
                            }
                        )
                    }

                    // Оверлей кадрирования
                    if (activeTool == EditorTools.CROP) {
                        AdvancedCropOverlay(
                            currentSettings = currentSettings,
                            onCropApply = { newRect ->
                                updateSettings(
                                    currentSettings.copy(
                                        cropRect = newRect,
                                        aspectRatio = (newRect.width / newRect.height)
                                    )
                                )
                                activeTool = EditorTools.ADJUST
                            }
                        )
                    }
                }
            }

            // НИЖНЯЯ ПАНЕЛЬ (без изменений)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditorToolItem(
                            Icons.Default.Tune,
                            "Adjust",
                            activeTool == EditorTools.ADJUST
                        ) { activeTool = EditorTools.ADJUST }
                        EditorToolItem(
                            Icons.Default.AutoAwesome,
                            "Filter",
                            activeTool == EditorTools.FILTERS
                        ) { activeTool = EditorTools.FILTERS }
                        EditorToolItem(
                            Icons.Default.Crop,
                            "Crop",
                            activeTool == EditorTools.CROP
                        ) { activeTool = EditorTools.CROP }
                        EditorToolItem(
                            Icons.Default.TextFields,
                            "Text",
                            activeTool == EditorTools.TEXT
                        ) { activeTool = EditorTools.TEXT }
                        EditorToolItem(
                            Icons.Default.Category,
                            "Shapes",
                            activeTool == EditorTools.SHAPES
                        ) { activeTool = EditorTools.SHAPES }
                    }
                    Box(
                        modifier = Modifier
                            .height(240.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        when (activeTool) {
                            EditorTools.ADJUST -> AdjustPanel(currentSettings) { updateSettings(it) }
                            EditorTools.FILTERS -> FilterRow(photoUri) { m, n ->
                                updateSettings(
                                    currentSettings.copy(selectedFilter = m, filterName = n)
                                )
                            }

                            EditorTools.CROP -> CropPanel(currentSettings) { updateLiveSettings(it) }
                            EditorTools.TEXT -> TextControlPanel(
                                settings = currentSettings,
                                selectedTextId = selectedTextId,
                                onUpdate = { updateSettings(it) },
                                onClose = { selectedTextId = null }
                            )

                            EditorTools.SHAPES -> ShapeControlPanel(
                                settings = currentSettings,
                                selectedShapeId = selectedShapeId,
                                onUpdate = { updateSettings(it) },
                                onClose = { selectedShapeId = null }
                            )
                        }
                    }
                }
            }
        }

        if (showExportDialog) {
            IosExportDialog(
                onDismiss = { showExportDialog = false },
                onGoToGallery = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    showExportDialog = false
                }
            )
        }

        if (isExporting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iOSBlue)
            }
        }
    }
}
