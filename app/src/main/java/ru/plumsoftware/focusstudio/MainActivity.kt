package ru.plumsoftware.focusstudio

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ru.plumsoftware.focusstudio.data.AdsConfig
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.PhotoEditorScreen
import ru.plumsoftware.focusstudio.ui.screen.editor.video.VideoEditorScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                Color.BLACK
            ),
            navigationBarStyle = SystemBarStyle.dark(
                Color.BLACK
            )
        )
        setContent {

            var isAdsLoading by remember { mutableStateOf(false) }

            FocusTheme(darkTheme = false) {
                val navController = rememberNavController()

                LaunchedEffect(key1 = Unit) {
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
                            Uri.decode(
                                it
                            ).toUri()
                        }
                        VideoEditorScreen(
                            videoUri = uri,
                            onCancel = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}