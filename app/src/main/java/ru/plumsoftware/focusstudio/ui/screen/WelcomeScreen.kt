package ru.plumsoftware.focusstudio.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.Routes
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue
import ru.plumsoftware.focusstudio.ui.theme.iOSPurple

@Composable
fun WelcomeScreen(navController: NavController, isAdsLoading: Boolean) {

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedUri = Uri.encode(uri.toString())
            navController.navigate("${Routes.PHOTO_EDITOR}/$encodedUri")
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val encodedUri = Uri.encode(it.toString())
            navController.navigate("${Routes.VIDEO_EDITOR}/$encodedUri")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Заменяем системный фон на жесткий черный в стиле iOS
                .background(Color.Black)
                .statusBarsPadding()
                .padding(FocusDesign.paddingLarge)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(FocusDesign.mainSpacing))

            // Badge "Профессиональная студия"
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent,
                border = BorderStroke(FocusDesign.badgeStrokeWidth, iOSBlue),
                modifier = Modifier.padding(bottom = FocusDesign.paddingMedium)
            ) {
                Text(
                    text = stringResource(R.string.prof_studio).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = iOSBlue,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Заголовок
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )

            Text(
                text = stringResource(R.string.subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = AppleGray,
                modifier = Modifier.padding(top = FocusDesign.paddingSmall)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Карточки выбора
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingMedium)
            ) {
                SelectionCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.video_title),
                    desc = stringResource(R.string.video_desc),
                    icon = Icons.Default.Videocam,
                    iconColor = iOSBlue,
                    onClick = { videoPickerLauncher.launch("video/*") }
                )
                SelectionCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.photo_title),
                    desc = stringResource(R.string.photo_desc),
                    icon = Icons.Default.Image,
                    iconColor = iOSPurple,
                    onClick = {
                        photoPickerLauncher.launch("image/*")
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Нижние бейджи (4K RAW и т.д.)
            TechInfoRow()
        }

        AnimatedVisibility(
            visible = isAdsLoading,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterVertically
                    )
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = "Загрузка рекламы",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionCard(
    modifier: Modifier,
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(FocusDesign.cardHeight)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        // Используем DarkSurface как во втором экране
        color = DarkSurface
    ) {
        Column(
            modifier = Modifier.padding(FocusDesign.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(FocusDesign.iconBoxSize),
                shape = RoundedCornerShape(FocusDesign.cornerMedium),
                color = iconColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(FocusDesign.paddingSmall))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = AppleGray // Используем мягкий серый для описания
            )
        }
    }
}

@Composable
fun TechInfoRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val techs = listOf(R.string.badge_4k, R.string.badge_60fps, R.string.badge_logc)
        techs.forEachIndexed { index, item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(item),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppleGray
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(FocusDesign.techUnderlineWidth)
                        .height(FocusDesign.techUnderlineHeight)
                        .background(AppleGray.copy(alpha = 0.3f))
                )
            }
            if (index < techs.size - 1) {
                Spacer(modifier = Modifier.width(FocusDesign.paddingLarge))
            }
        }
    }
}

@Composable
fun LanguageToggle() {
    var selectedLanguage by remember { mutableStateOf("RU") }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f)) // Тонкая подложка для всего ряда
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageButton(
            text = "RU",
            isSelected = selectedLanguage == "RU",
            onClick = { selectedLanguage = "RU" }
        )
        LanguageButton(
            text = "US",
            isSelected = selectedLanguage == "US",
            onClick = { selectedLanguage = "US" }
        )
    }
}

@Composable
private fun LanguageButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Color.White.copy(alpha = 0.15f) // Светлее для активного
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) Color.White else AppleGray

    Box(
        modifier = Modifier
            .size(FocusDesign.languageToggleSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = textColor
        )
    }
}