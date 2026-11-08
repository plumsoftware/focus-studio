package ru.plumsoftware.focusstudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.Lifecycle
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
import ru.plumsoftware.focusstudio.ui.screen.WelcomeScreen
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.collage.CollageEditorScreen
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.PhotoEditorScreen
import ru.plumsoftware.focusstudio.ui.screen.editor.video.screen.VideoEditorScreen
import ru.plumsoftware.focusstudio.ui.theme.FocusTheme
import ru.plumsoftware.focusstudio.ui.theme.Routes

class MainActivity : ComponentActivity() {

    private var onReturnFromSettings: (() -> Unit)? = null

    override fun onResume() {
        super.onResume()
        onReturnFromSettings?.let { callback ->
            onReturnFromSettings = null
            callback()
        }
    }

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

            // Загружаем App Open только после завершения всех системных запросов разрешений.
            val startLoadingAds: () -> Unit = {
                val canShowLaunchAd = AppPrefs.shouldShowLaunchAd(context) &&
                    AdsConfig.isEnabled(AdsConfig.OPEN_ADS_ID)

                if (canShowLaunchAd && !isAdsLoading) {
                    isAdsLoading = true
                    val loader = AppOpenAdLoader(application)
                    val request = AdRequest.Builder(AdsConfig.OPEN_ADS_ID).build()

                    loader.loadAd(request, object : AppOpenAdLoadListener {
                        override fun onAdLoaded(appOpenAd: AppOpenAd) {
                            isAdsLoading = false
                            appOpenAd.setAdEventListener(object : AppOpenAdEventListener {
                                override fun onAdClicked() {}
                                override fun onAdDismissed() {}
                                override fun onAdFailedToShow(adError: AdError) {}
                                override fun onAdImpression(impressionData: ImpressionData?) {}
                                override fun onAdShown() {}
                            })

                            // Не показываем рекламу поверх системного окна или при закрытой Activity.
                            if (!isFinishing && !isDestroyed &&
                                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                            ) {
                                appOpenAd.show(this@MainActivity)
                            }
                        }

                        override fun onAdFailedToLoad(error: AdRequestError) {
                            isAdsLoading = false
                        }
                    })
                } else {
                    AppPrefs.markFirstLaunchComplete(context)
                }
            }

            val permissionsToRequest = remember {
                buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                        add(Manifest.permission.READ_MEDIA_IMAGES)
                        add(Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        Manifest.permission.WRITE_EXTERNAL_STORAGE !in this
                    ) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }.filter { permission ->
                    ContextCompat.checkSelfPermission(context, permission) !=
                        PackageManager.PERMISSION_GRANTED
                }.toTypedArray()
            }

            val finishPermissionFlow: () -> Unit = {
                startLoadingAds()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val hasDeniedPermission = permissionsMap.values.any { granted -> !granted }
                if (hasDeniedPermission) {
                    // Не запускаем загрузку рекламы, пока открыто наше окно с объяснением разрешений.
                    showPermissionDialog = true
                } else {
                    finishPermissionFlow()
                }
            }

            LaunchedEffect(Unit) {
                delay(300)
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest)
                } else {
                    finishPermissionFlow()
                }
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
                                onCancel = { navController.popBackStack() }
                            )
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
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.COLLAGE_EDITOR) {
                            CollageEditorScreen(
                                onCancel = { navController.popBackStack() },
                                onOpenInEditor = { collageUri ->
                                    val encodedUri = Uri.encode(collageUri.toString())
                                    navController.navigate("${Routes.PHOTO_EDITOR}/$encodedUri") {
                                        popUpTo(Routes.COLLAGE_EDITOR) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    if (showPermissionDialog) {
                        IosPermissionDialog(
                            onDismiss = {
                                showPermissionDialog = false
                                finishPermissionFlow()
                            },
                            onGoToSettings = {
                                showPermissionDialog = false
                                // Если пользователь открыл настройки, рекламу загружаем только
                                // после возвращения в приложение.
                                onReturnFromSettings = finishPermissionFlow
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