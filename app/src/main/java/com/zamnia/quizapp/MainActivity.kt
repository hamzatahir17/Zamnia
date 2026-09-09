package com.zamnia.quizapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zamnia.quizapp.ui.auth.AuthViewModel
import com.zamnia.quizapp.ui.quiz.QuizViewModel
import com.zamnia.quizapp.ui.zamnia.*
import com.zamnia.quizapp.ui.theme.ZamniaTheme

import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        printAppSignature()
        
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val userProfile by authViewModel.userProfile.collectAsState()
            val activeThemeId = userProfile?.activeThemeId ?: "default"

            // Fast direct routing based on session validation (No artificial Compose splash delay)
            val startDestination = remember { mutableStateOf<String?>(null) }

            // Keep native splash screen visible until destination is determined (Eliminates white flash)
            splashScreen.setKeepOnScreenCondition { startDestination.value == null }

            LaunchedEffect(Unit) {
                val isValid = authViewModel.validateSession()
                startDestination.value = if (isValid) "dashboard" else "onboarding"
            }

            ZamniaTheme(activeThemeId = activeThemeId) {
                val navController = rememberNavController()
                val currentDestination = startDestination.value

                if (currentDestination != null) {
                    NavHost(
                        navController = navController,
                        startDestination = currentDestination
                    ) {
                        composable("onboarding") {
                            ZamniaOnboardingScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                },
                                viewModel = authViewModel
                            )
                        }
                    composable("dashboard") {
                        ZamniaDashboardScreen(
                            onNavigateToWallet = { navController.navigate("wallet") },
                            onNavigateToQuiz = { packageId -> 
                                val route = if (packageId != null) "quiz_flow/$packageId" else "quiz_flow/general"
                                navController.navigate(route)
                            },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToPacks = { navController.navigate("packs") },
                            onLogout = {
                                navController.navigate("onboarding") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("wallet") {
                        ZamniaWalletScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToHub = { navController.navigate("dashboard") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToPacks = { navController.navigate("packs") }
                        )
                    }
                    composable("settings") {
                        ZamniaSettingsScreen(
                            onNavigateToHub = { navController.navigate("dashboard") },
                            onNavigateToWallet = { navController.navigate("wallet") },
                            onNavigateToPacks = { navController.navigate("packs") },
                            onLogout = {
                                navController.navigate("onboarding") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("packs") {
                        ZamniaPacksScreen(
                            onNavigateToHub = { navController.navigate("dashboard") },
                            onNavigateToWallet = { navController.navigate("wallet") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToQuiz = { packageId -> 
                                navController.navigate("quiz_flow/$packageId")
                            }
                        )
                    }

                    navigation(
                        startDestination = "quiz/{packageId}",
                        route = "quiz_flow/{packageId}"
                    ) {
                        composable(
                            route = "quiz/{packageId}",
                            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val packageId = backStackEntry.arguments?.getString("packageId")
                            val idToPass = if (packageId == "general") null else packageId
                            
                            // Scope ViewModel to the 'quiz_flow' route
                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("quiz_flow/{packageId}")
                            }
                            val quizViewModel: QuizViewModel = viewModel(parentEntry)
                            
                            ZamniaQuizSessionScreen(
                                packageId = idToPass,
                                onBack = { navController.popBackStack() },
                                onQuizFinished = { score, total, coins ->
                                    if (navController.currentDestination?.route?.contains("quiz") == true) {
                                        navController.navigate("results/$score/$total/$coins/$packageId")
                                    }
                                },
                                viewModel = quizViewModel
                            )
                        }
                        composable(
                            route = "results/{score}/{total}/{coins}/{packageId}",
                            arguments = listOf(
                                navArgument("score") { type = NavType.IntType },
                                navArgument("total") { type = NavType.IntType },
                                navArgument("coins") { type = NavType.IntType },
                                navArgument("packageId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val score = backStackEntry.arguments?.getInt("score") ?: 0
                            val total = backStackEntry.arguments?.getInt("total") ?: 0
                            val coins = backStackEntry.arguments?.getInt("coins") ?: 0
                            val packageId = backStackEntry.arguments?.getString("packageId")
                            val idToPass = if (packageId == "general") null else packageId
                            
                            val parentEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("quiz_flow/{packageId}")
                            }
                            val quizViewModel: QuizViewModel = viewModel(parentEntry)
                            val responses by quizViewModel.quizResponses.collectAsState()

                            ZamniaQuizResultsScreen(
                                score = score,
                                total = total,
                                coins = coins,
                                responses = responses,
                                onPlayAgain = {
                                    // Reset the state first to prevent immediate navigation back to results
                                    quizViewModel.startQuiz(0, idToPass)
                                    navController.popBackStack() 
                                },
                                onReturnToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

    private fun printAppSignature() {
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }

            signatures?.forEach { sig ->
                val md = MessageDigest.getInstance("SHA-1")
                md.update(sig.toByteArray())
                val digest = md.digest()
                val hexString = digest.joinToString(":") { String.format("%02X", it) }
                Log.e("ZamniaAppSignature", "ACTUAL RUNTIME SHA-1: $hexString")
            }
        } catch (e: Exception) {
            Log.e("ZamniaAppSignature", "Error getting signature", e)
        }
    }
}

