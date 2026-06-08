package ru.plumsoftware.focusstudio.ui.screen

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.copyUriToCache
import ru.plumsoftware.focusstudio.data.RecentProject
import ru.plumsoftware.focusstudio.data.RecentProjectsHelper
import ru.plumsoftware.focusstudio.ui.theme.AppleGray
import ru.plumsoftware.focusstudio.ui.theme.DarkSurface
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.Routes
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue
import ru.plumsoftware.focusstudio.ui.theme.iOSPurple

@Composable
fun WelcomeScreen(navController: NavController, isAdsLoading: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isImportingFile by remember { mutableStateOf(false) }
    var recentProjects by remember { mutableStateOf<List<RecentProject>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recentProjects = RecentProjectsHelper.loadRecent(context)
    }

    val openEditor: (Uri, Boolean) -> Unit = { uri, isVideo ->
        isImportingFile = true
        scope.launch {
            val cachedUri = withContext(Dispatchers.IO) {
                copyUriToCache(context, uri)
            }
            isImportingFile = false
            if (cachedUri != null) {
                val encodedUri = Uri.encode(cachedUri.toString())
                val route = if (isVideo) Routes.VIDEO_EDITOR else Routes.PHOTO_EDITOR
                navController.navigate("$route/$encodedUri")
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) openEditor(uri, false)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) openEditor(uri, true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocusDesign.paddingMedium, vertical = FocusDesign.paddingSmall),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = AppleGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FocusDesign.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(FocusDesign.paddingMedium))

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

                Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))

                FeatureHighlightsRow()

                Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))

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
                        onClick = { photoPickerLauncher.launch("image/*") }
                    )
                }

                if (recentProjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))
                    RecentProjectsSection(
                        projects = recentProjects,
                        onProjectClick = { project -> openEditor(project.uri, project.isVideo) },
                        onViewAll = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                type = "*/*"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(FocusDesign.paddingLarge))
            }
        }

        AnimatedVisibility(
            visible = isAdsLoading || isImportingFile,
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
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = if (isAdsLoading) {
                            stringResource(R.string.loading_ads)
                        } else {
                            stringResource(R.string.loading_import)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        if (showSettings) {
            SettingsBottomSheet(onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun FeatureHighlightsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FeatureItem(Icons.Outlined.CameraAlt, stringResource(R.string.badge_4k))
        FeatureItem(Icons.Outlined.Movie, stringResource(R.string.badge_60fps))
        FeatureItem(Icons.Outlined.Palette, stringResource(R.string.badge_logc))
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppleGray,
            modifier = Modifier.size(FocusDesign.featureIconSize)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = AppleGray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun RecentProjectsSection(
    projects: List<RecentProject>,
    onProjectClick: (RecentProject) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_title),
                fontSize = 13.sp,
                color = AppleGray
            )
            Text(
                text = stringResource(R.string.recent_all),
                fontSize = 13.sp,
                color = iOSBlue,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }

        Spacer(modifier = Modifier.height(FocusDesign.paddingSmall))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FocusDesign.paddingSmall)
        ) {
            items(projects, key = { it.uri.toString() }) { project ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onProjectClick(project) }
                ) {
                    AsyncImage(
                        model = project.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(FocusDesign.recentThumbSize)
                            .clip(RoundedCornerShape(FocusDesign.cornerExtraSmall))
                    )
                    Text(
                        text = project.dateLabel,
                        fontSize = 11.sp,
                        color = AppleGray,
                        modifier = Modifier.padding(top = 4.dp)
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
                color = AppleGray
            )
        }
    }
}
