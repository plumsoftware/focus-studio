package ru.plumsoftware.focusstudio

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.plumsoftware.focusstudio.ui.screen.WelcomeScreen
import ru.plumsoftware.focusstudio.ui.theme.FocusTheme
import ru.plumsoftware.focusstudio.ui.theme.Routes
import androidx.core.net.toUri
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import kotlinx.coroutines.delay
import ru.plumsoftware.focusstudio.data.AdsConfig
import ru.plumsoftware.focusstudio.data.AppPrefs
import ru.plumsoftware.focusstudio.ui.screen.IosPermissionDialog
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.PhotoEditorScreen
import ru.plumsoftware.focusstudio.ui.screen.editor.video.screen.VideoEditorScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )
        setContent {
            var isAdsLoading by remember { mutableStateOf(false) }
            var showPermissionDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val navController = rememberNavController()
            val shouldShowLaunchAd = remember { AppPrefs.shouldShowLaunchAd(context) }

            // 1. Выносим логику загрузки рекламы в отдельную функцию
            val startLoadingAds: () -> Unit = {
                isAdsLoading = true
                val appOpenAdLoader = AppOpenAdLoader(application)
                val adRequest = AdRequest.Builder(AdsConfig.OPEN_ADS_ID).build()

                val appOpenAdLoadListener = object : AppOpenAdLoadListener {
                    override fun onAdLoaded(appOpenAd: AppOpenAd) {
                        isAdsLoading = false
                        appOpenAd.setAdEventListener(object : AppOpenAdEventListener {
                            override fun onAdClicked() {}
                            override fun onAdDismissed() {}
                            override fun onAdFailedToShow(adError: AdError) {}
                            override fun onAdImpression(impressionData: ImpressionData?) {}
                            override fun onAdShown() {}
                        })
                        appOpenAd.show(this@MainActivity)
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        isAdsLoading = false
                    }
                }
                appOpenAdLoader.loadAd(adRequest, appOpenAdLoadListener)
            }

            // 2. Настраиваем разрешения
            val permissionsToRequest = remember {
                mutableListOf<String>().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                        add(Manifest.permission.READ_MEDIA_IMAGES)
                        add(Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE) // Уже есть — ок
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (!contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }.toTypedArray()
            }

            // 3. Лаунчер теперь запускает рекламу ПОСЛЕ закрытия системного окна
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val isAnyDenied = permissionsMap.values.contains(false)
                if (isAnyDenied) {
                    showPermissionDialog = true
                }

                if (shouldShowLaunchAd) {
                    startLoadingAds()
                } else {
                    AppPrefs.markFirstLaunchComplete(context)
                }
            }

            // 4. При старте запускаем ТОЛЬКО запрос разрешений
            LaunchedEffect(key1 = Unit) {
                delay(300)
                permissionLauncher.launch(permissionsToRequest)
            }

            FocusTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.WELCOME
                    ) {
                        composable(Routes.WELCOME) {
                            WelcomeScreen(
                                navController = navController,
                                isAdsLoading = isAdsLoading
                            )
                        }

                        composable(
                            route = "${Routes.PHOTO_EDITOR}/{photoUri}",
                            arguments = listOf(navArgument("photoUri") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val uriString = backStackEntry.arguments?.getString("photoUri")
                            val uri = uriString?.let { Uri.decode(it).toUri() }
                            PhotoEditorScreen(
                                photoUri = uri,
                                onCancel = { navController.popBackStack() })
                        }

                        composable(
                            route = "${Routes.VIDEO_EDITOR}/{videoUri}",
                            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val uri = backStackEntry.arguments?.getString("videoUri")?.let {
                                Uri.decode(it).toUri()
                            }
                            VideoEditorScreen(
                                videoUri = uri,
                                onCancel = { navController.popBackStack() })
                        }
                    }

                    // Диалог Rationale (iOS Style) показывается поверх навигации
                    if (showPermissionDialog) {
                        IosPermissionDialog(
                            onDismiss = { showPermissionDialog = false },
                            onGoToSettings = {
                                showPermissionDialog = false
                                openAppSettings(context)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openAppSettings(context: android.content.Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}