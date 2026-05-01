package ru.plumsoftware.focusstudio.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
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
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.Routes

@Composable
fun WelcomeScreen(navController: NavController) {

    // Лаунчер для выбора фото
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedUri = Uri.encode(uri.toString())
            navController.navigate("${Routes.PHOTO_EDITOR}/$encodedUri")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(FocusDesign.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Переключатель языков
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            LanguageToggle()
        }

        Spacer(modifier = Modifier.height(FocusDesign.mainSpacing))

        // Badge
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Transparent,
            border = BorderStroke(FocusDesign.badgeStrokeWidth, MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(bottom = FocusDesign.paddingMedium)
        ) {
            Text(
                text = stringResource(R.string.prof_studio).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Заголовок
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
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
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate(Routes.VIDEO_EDITOR) }
            )
            SelectionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.photo_title),
                desc = stringResource(R.string.photo_desc),
                icon = Icons.Default.Image,
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                    photoPickerLauncher.launch("image/*")
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Нижние бейджи
        TechInfoRow()
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
        color = MaterialTheme.colorScheme.surface
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

            Text(text = title, style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(FocusDesign.paddingSmall))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(FocusDesign.techUnderlineWidth)
                        .height(FocusDesign.techUnderlineHeight)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
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
    // В реальном приложении это состояние должно приходить из ViewModel или LocalLocale
    var selectedLanguage by remember { mutableStateOf("RU") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingSmall),
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
    // Цвета подложки в стиле iOS (размытый серый/белый с прозрачностью)
    val backgroundColor = if (isSelected) {
        // На темном фоне это светло-серый с прозрачностью, на светлом — темнее
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    // Цвет текста
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        // Используем AppleGray (onSurfaceVariant) для неактивного состояния
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(FocusDesign.languageToggleSize) // 38.dp из ваших констант
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            // Используем типографику из темы (SF-Pro)
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.sp
            ),
            color = textColor
        )
    }
}