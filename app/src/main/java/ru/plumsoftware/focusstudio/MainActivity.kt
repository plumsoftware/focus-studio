package ru.plumsoftware.focusstudio

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.plumsoftware.focusstudio.ui.screen.WelcomeScreen
import ru.plumsoftware.focusstudio.ui.theme.FocusTheme
import ru.plumsoftware.focusstudio.ui.theme.Routes
import androidx.core.net.toUri
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.screen.PhotoEditorScreen
import ru.plumsoftware.focusstudio.ui.screen.editor.video.VideoEditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.WELCOME
                ) {
                    composable(Routes.WELCOME) { WelcomeScreen(navController) }

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