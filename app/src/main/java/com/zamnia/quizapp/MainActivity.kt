package com.zamnia.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZamniaTheme {
                val navController = rememberNavController()
                // Shared AuthViewModel for the entire app to avoid re-loading session
                val authViewModel: AuthViewModel = viewModel()
                
                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        
                        // Start validation IMMEDIATELY when Splash composable enters the screen
                        // This result will be ready by the time animation finishes.
                        val validationResult = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Boolean?>(null) }
                        
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            validationResult.value = authViewModel.validateSession()
                        }

                        ZamniaSplashScreen(
                            onSplashFinished = {
                                scope.launch {
                                    // If result is not ready (slow network), it will wait here briefly.
                                    // If already ready, it navigates instantly.
                                    while (validationResult.value == null) {
                                        kotlinx.coroutines.delay(100)
                                    }
                                    
                                    if (validationResult.value == true) {
                                        navController.navigate("dashboard") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("onboarding") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        )
                    }
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
                                    navController.navigate("results/$score/$total/$coins")
                                },
                                viewModel = quizViewModel
                            )
                        }
                        composable(
                            route = "results/{score}/{total}/{coins}",
                            arguments = listOf(
                                navArgument("score") { type = NavType.IntType },
                                navArgument("total") { type = NavType.IntType },
                                navArgument("coins") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val score = backStackEntry.arguments?.getInt("score") ?: 0
                            val total = backStackEntry.arguments?.getInt("total") ?: 0
                            val coins = backStackEntry.arguments?.getInt("coins") ?: 0
                            
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
