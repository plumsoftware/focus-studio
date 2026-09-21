package ru.plumsoftware.focusstudio.ui.screen.editor.photo.collage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.copyUriToCache
import ru.plumsoftware.focusstudio.ui.theme.AccentEnd
import ru.plumsoftware.focusstudio.ui.theme.AccentStart
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.GradientAccent
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CollageEditorScreen(
    onCancel: () -> Unit,
    onOpenInEditor: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember {
        mutableStateOf(
            CollageState(
                template = CollageTemplates.forTwo.first(),
                cells = List(2) { CollageCell() }
            )
        )
    }
    var activeTab by remember { mutableIntStateOf(0) } // 0 — макет, 1 — стиль
    var isBusy by remember { mutableStateOf(false) }
    var pendingCellIndex by remember { mutableIntStateOf(-1) }

    fun setCell(index: Int, transform: (CollageCell) -> CollageCell) {
        state = state.copy(
            cells = state.cells.mapIndexed { i, c -> if (i == index) transform(c) else c }
        )
    }

    // --- Выбор одного фото для конкретной ячейки ---
    val cellPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val index = pendingCellIndex
        if (uri != null && index >= 0) {
            scope.launch {
                isBusy = true
                val cached = withContext(Dispatchers.IO) { copyUriToCache(context, uri) }
                isBusy = false
                if (cached != null) {
                    setCell(index) { it.copy(uri = cached, zoom = 1f, panX = 0f, panY = 0f) }
                }
            }
        }
    }

    // --- Первичный выбор нескольких фото при входе на экран ---
    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isBusy = true
                val cached = withContext(Dispatchers.IO) {
                    uris.take(4).mapNotNull { copyUriToCache(context, it) }
                }
                isBusy = false
                if (cached.isNotEmpty()) {
                    val count = cached.size.coerceIn(2, 4)
                    val tpl = CollageTemplates.byCount(count).first()
                    state = state.copy(
                        template = tpl,
                        cells = List(count) { idx -> CollageCell(uri = cached.getOrNull(idx)) }
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        multiPicker.launch("image/*")
    }

    val filledCount = state.cells.count { it.uri != null }
    val canProceed = filledCount >= 2 && !isBusy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CollageTopBar(
                onCancel = onCancel,
                canProceed = canProceed,
                onNext = {
                    if (canProceed) {
                        scope.launch {
                            isBusy = true
                            val uri = withContext(Dispatchers.IO) {
                                composeCollageToCache(context, state)
                            }
                            isBusy = false
                            if (uri != null) onOpenInEditor(uri)
                        }
                    }
                }
            )

            // --- ХОЛСТ ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CollageCanvas(
                    state = state,
                    onCellTap = { index ->
                        pendingCellIndex = index
                        cellPicker.launch("image/*")
                    },
                    onCellTransform = { index, zoomFactor, pan, cellW, cellH ->
                        setCell(index) { c ->
                            val nz = (c.zoom * zoomFactor).coerceIn(1f, 4f)
                            val clamp = max(0f, (nz - 1f) / 2f)
                            val npx = (c.panX + pan.x / cellW).coerceIn(-clamp, clamp)
                            val npy = (c.panY + pan.y / cellH).coerceIn(-clamp, clamp)
                            c.copy(zoom = nz, panX = npx, panY = npy)
                        }
                    },
                    onCellRemove = { index ->
                        setCell(index) { CollageCell() }
                    }
                )

                if (filledCount < 2) {
                    Text(
                        text = stringResource(R.string.collage_min_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // --- НИЖНЯЯ ПАНЕЛЬ ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = FocusDesign.paddingMedium)
                        .padding(bottom = FocusDesign.paddingMedium)
                ) {
                    CollageTabs(activeTab) { activeTab = it }

                    Box(modifier = Modifier.height(210.dp)) {
                        when (activeTab) {
                            0 -> LayoutPanel(
                                state = state,
                                onCountSelected = { count ->
                                    val tpl = CollageTemplates.byCount(count).first()
                                    state = CollageTemplates.applyTemplate(state, tpl)
                                },
                                onTemplateSelected = { tpl ->
                                    state = CollageTemplates.applyTemplate(state, tpl)
                                },
                                onAspectSelected = { state = state.copy(aspect = it) }
                            )

                            else -> StylePanel(
                                state = state,
                                onGapChange = { state = state.copy(gap = it) },
                                onCornerChange = { state = state.copy(corner = it) },
                                onBackgroundChange = { state = state.copy(background = it) }
                            )
                        }
                    }
                }
            }
        }

        if (isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = iOSBlue)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.collage_building),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageTopBar(
    onCancel: () -> Unit,
    canProceed: Boolean,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(FocusDesign.topBarHeight)
            .padding(horizontal = FocusDesign.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.btn_cancel),
            color = AccentStart,
            modifier = Modifier.clickable { onCancel() },
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = stringResource(R.string.collage_title),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .then(
                    if (canProceed) Modifier.background(GradientAccent)
                    else Modifier.background(Color.White.copy(alpha = 0.12f))
                )
                .clickable(enabled = canProceed) { onNext() }
        ) {
            Text(
                text = stringResource(R.string.collage_next),
                color = if (canProceed) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun CollageCanvas(
    state: CollageState,
    onCellTap: (Int) -> Unit,
    onCellTransform: (Int, Float, Offset, Float, Float) -> Unit,
    onCellRemove: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(FocusDesign.paddingMedium),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val availW = constraints.maxWidth.toFloat()
        val availH = constraints.maxHeight.toFloat()
        val ratio = state.aspect.ratio

        // Вписываем холст нужного формата в доступную область.
        val canvasW: Float
        val canvasH: Float
        if (availW / availH > ratio) {
            canvasH = availH
            canvasW = availH * ratio
        } else {
            canvasW = availW
            canvasH = availW / ratio
        }

        val minPx = min(canvasW, canvasH)
        val gapPx = state.gap * 0.05f * minPx
        val cornerPx = state.corner * 0.10f * minPx

        Box(
            modifier = Modifier.size(
                width = with(density) { canvasW.toDp() },
                height = with(density) { canvasH.toDp() }
            )
        ) {
            state.template.cells.forEachIndexed { index, rect ->
                val cell = state.cells.getOrNull(index) ?: CollageCell()

                val leftPx = rect.left * canvasW + gapPx / 2f
                val topPx = rect.top * canvasH + gapPx / 2f
                val cellWpx = (rect.right - rect.left) * canvasW - gapPx
                val cellHpx = (rect.bottom - rect.top) * canvasH - gapPx
                if (cellWpx <= 1f || cellHpx <= 1f) return@forEachIndexed

                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .size(
                            width = with(density) { cellWpx.toDp() },
                            height = with(density) { cellHpx.toDp() }
                        )
                        .clip(RoundedCornerShape(with(density) { cornerPx.toDp() }))
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    CollageCellContent(
                        cell = cell,
                        cellWpx = cellWpx,
                        cellHpx = cellHpx,
                        onTap = { onCellTap(index) },
                        onTransform = { z, pan -> onCellTransform(index, z, pan, cellWpx, cellHpx) },
                        onRemove = { onCellRemove(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageCellContent(
    cell: CollageCell,
    cellWpx: Float,
    cellHpx: Float,
    onTap: () -> Unit,
    onTransform: (Float, Offset) -> Unit,
    onRemove: () -> Unit
) {
    if (cell.uri == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTap() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GradientAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.collage_empty_hint),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.collage_empty_hint),
                    color = AppleGray,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = cell.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = cell.zoom
                        scaleY = cell.zoom
                        translationX = cell.panX * cellWpx
                        translationY = cell.panY * cellHpx
                    }
                    .pointerInput(cell.uri, cellWpx, cellHpx) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onTransform(zoom, pan)
                        }
                    }
            )

            // Кнопка удаления фото из ячейки
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_collage_remove),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CollageTabs(active: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        stringResource(R.string.collage_tab_layout),
        stringResource(R.string.collage_tab_style)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FocusDesign.paddingSmall),
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingSmall)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(FocusDesign.cornerFull))
                    .then(
                        if (selected) Modifier.background(GradientAccent)
                        else Modifier.background(Color.White.copy(alpha = 0.06f))
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (selected) Color.White else AppleGray,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LayoutPanel(
    state: CollageState,
    onCountSelected: (Int) -> Unit,
    onTemplateSelected: (CollageTemplate) -> Unit,
    onAspectSelected: (CollageAspect) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Количество фото
        PanelLabel(stringResource(R.string.collage_photos))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingSmall)
        ) {
            CollageTemplates.availableCounts.forEach { count ->
                val selected = state.template.count == count
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(FocusDesign.cornerMedium))
                        .then(
                            if (selected) Modifier.background(Color.White.copy(alpha = 0.14f))
                            else Modifier.background(Color.White.copy(alpha = 0.05f))
                        )
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) AccentStart else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(FocusDesign.cornerMedium)
                        )
                        .clickable { onCountSelected(count) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        color = if (selected) Color.White else AppleGray,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(FocusDesign.paddingMedium))

        // Раскладки для выбранного количества
        PanelLabel(stringResource(R.string.collage_layout))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingSmall)
        ) {
            items(CollageTemplates.byCount(state.template.count)) { template ->
                val selected = template.id == state.template.id
                TemplateThumb(
                    template = template,
                    selected = selected,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }

        Spacer(Modifier.height(FocusDesign.paddingMedium))

        // Формат холста
        PanelLabel(stringResource(R.string.collage_aspect))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingSmall)
        ) {
            items(CollageAspect.entries.toList()) { aspect ->
                val selected = aspect == state.aspect
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FocusDesign.cornerFull))
                        .then(
                            if (selected) Modifier.background(GradientAccent)
                            else Modifier.background(Color.White.copy(alpha = 0.06f))
                        )
                        .clickable { onAspectSelected(aspect) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(aspect.labelRes),
                        color = if (selected) Color.White else AppleGray,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateThumb(
    template: CollageTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(FocusDesign.cornerExtraSmall))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AccentStart else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(FocusDesign.cornerExtraSmall)
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gap = 3f
            template.cells.forEach { r ->
                val left = r.left * size.width + gap / 2f
                val top = r.top * size.height + gap / 2f
                val w = (r.right - r.left) * size.width - gap
                val h = (r.bottom - r.top) * size.height - gap
                if (w > 0f && h > 0f) {
                    drawRoundRect(
                        color = if (selected) AccentEnd else Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StylePanel(
    state: CollageState,
    onGapChange: (Float) -> Unit,
    onCornerChange: (Float) -> Unit,
    onBackgroundChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        StyleSlider(
            label = stringResource(R.string.collage_gap),
            value = state.gap,
            onValueChange = onGapChange
        )
        StyleSlider(
            label = stringResource(R.string.collage_corner),
            value = state.corner,
            onValueChange = onCornerChange
        )

        Spacer(Modifier.height(FocusDesign.paddingSmall))
        PanelLabel(stringResource(R.string.label_background))
        CollageBackgroundRow(
            selected = state.background,
            onSelected = onBackgroundChange
        )
    }
}

@Composable
private fun StyleSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray
            )
            Text(
                text = "${(value * 100).roundToInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentStart,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = AccentStart,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun CollageBackgroundRow(
    selected: Color,
    onSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.White, Color(0xFFF2F2F7), Color(0xFF151517), Color.Black,
        Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
        Color(0xFF4CD964), Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFFAF52DE)
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = FocusDesign.paddingSmall)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(FocusDesign.colorDotSize)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected == color) 2.dp else 1.dp,
                        color = if (selected == color) AccentStart else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onSelected(color) }
            )
        }
    }
}

@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = AppleGray,
        modifier = Modifier.padding(bottom = FocusDesign.paddingSmall)
    )
}
