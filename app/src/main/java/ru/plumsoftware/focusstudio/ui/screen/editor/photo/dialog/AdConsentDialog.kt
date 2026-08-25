package ru.plumsoftware.focusstudio.ui.screen.editor.photo.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.theme.AccentStart
import ru.plumsoftware.focusstudio.ui.theme.AppleGray

/**
 * Окно перед рекламой при экспорте. Два состояния:
 * 1) Согласие — пользователь решает, смотреть рекламу или пропустить.
 * 2) Загрузка — после «Смотреть» ждём 3 секунды, затем вызываем [onWatch] (показ рекламы).
 *
 * Крестик и «Пропустить» вызывают [onSkip] — реклама не показывается.
 */
@Composable
fun AdConsentDialog(
    onSkip: () -> Unit,
    onWatch: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    // Ждём 3 секунды на этапе загрузки, затем отдаём управление на показ рекламы.
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(3000)
            onWatch()
        }
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onSkip() },
        properties = DialogProperties(
            // На этапе загрузки не даём закрыть тапом снаружи — только крестиком.
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1C1C1E).copy(alpha = 0.95f),
            modifier = Modifier.width(270.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Крестик в правом верхнем углу — всегда закрывает без показа рекламы.
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = AppleGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isLoading) {
                    LoadingContent()
                } else {
                    ConsentContent(
                        onSkip = onSkip,
                        onWatch = { isLoading = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentContent(onSkip: () -> Unit, onWatch: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 28.dp)
    ) {
        Text(
            text = stringResource(R.string.ad_consent_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ad_consent_message),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(36.dp))

        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

        Row(modifier = Modifier.height(44.dp).padding(top = 18.dp), horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onSkip() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ad_consent_skip),
                    color = AppleGray
                )
            }

            Divider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onWatch() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ad_consent_watch),
                    color = AccentStart,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 32.dp)
    ) {
        CircularProgressIndicator(color = AccentStart, strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.loading_ads_progress),
            color = Color.White,
            fontSize = 13.sp
        )
    }
}
