package com.docvaultbasic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.docvaultbasic.ui.screens.AllFilesScreen
import com.docvaultbasic.ui.screens.FolderSelectionScreen
import com.docvaultbasic.ui.screens.HomeScreen
import com.docvaultbasic.ui.screens.ImageEditorScreen
import com.docvaultbasic.ui.screens.ImportConfirmationScreen
import com.docvaultbasic.ui.screens.LockScreen
import com.docvaultbasic.ui.screens.OnboardingScreen
import com.docvaultbasic.ui.screens.PdfViewerScreen
import com.docvaultbasic.ui.screens.PinSetupScreen
import com.docvaultbasic.ui.screens.SearchScreen
import com.docvaultbasic.ui.screens.SettingsScreen
import com.docvaultbasic.ui.viewmodel.MainViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val startDestination = remember(mainViewModel.startDestination.value) {
        mainViewModel.startDestination.value
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.PinSetup.route) { PinSetupScreen(navController) }
        composable(Screen.Lock.route) { LockScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Search.route) { SearchScreen(navController) }
        composable(Screen.FolderSelection.route) { FolderSelectionScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.AllFiles.route) { AllFilesScreen(navController) }
        composable(
            route = Screen.ImportConfirmation.route,
            arguments = listOf(navArgument("fileUri") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("fileUri")
            if (uri != null) {
                ImportConfirmationScreen(uri, navController)
            }
        }
        composable(
            route = Screen.ImageEditor.route,
            arguments = listOf(navArgument("imageUri") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("imageUri")
            if (uri != null) {
                ImageEditorScreen(uri, navController)
            }
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(navArgument("documentId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")?.toIntOrNull()
            if (documentId != null) {
                PdfViewerScreen(documentId, navController)
            }
        }
    }
}
