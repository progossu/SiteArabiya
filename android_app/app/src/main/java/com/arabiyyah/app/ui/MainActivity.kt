package com.arabiyyah.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arabiyyah.app.audio.AudioPlayerManager
import com.arabiyyah.app.ui.components.LocalAudioPlayerManager
import com.arabiyyah.app.ui.navigation.Screen
import com.arabiyyah.app.ui.screens.CollocationsScreen
import com.arabiyyah.app.ui.screens.UnitDetailScreen
import com.arabiyyah.app.ui.screens.UnitListScreen
import com.arabiyyah.app.ui.screens.VocabularyScreen
import com.arabiyyah.app.ui.screens.VerbsScreen
import com.arabiyyah.app.ui.theme.ArabiyyahTheme
import com.arabiyyah.app.ui.theme.LocalThemeIsDark
import com.arabiyyah.app.ui.theme.LocalThemeToggle
import com.arabiyyah.app.ui.theme.ThemePreferences

open class MainActivity : ComponentActivity() {
    protected var audioPlayerManager: AudioPlayerManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val playerManager = remember {
                AudioPlayerManager(context).also { audioPlayerManager = it }
            }

            var isDarkTheme by rememberSaveable {
                mutableStateOf(ThemePreferences.getInitialDarkMode(context))
            }

            val toggleTheme = {
                val next = !isDarkTheme
                isDarkTheme = next
                ThemePreferences.saveDarkMode(context, next)
            }

            DisposableEffect(Unit) {
                onDispose {
                    playerManager.release()
                }
            }

            ArabiyyahTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(
                    LocalAudioPlayerManager provides playerManager,
                    LocalThemeIsDark provides isDarkTheme,
                    LocalThemeToggle provides toggleTheme
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ArabiyyahNavHost(audioPlayerManager = playerManager)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager?.release()
    }
}

@Composable
fun ArabiyyahNavHost(
    audioPlayerManager: AudioPlayerManager,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.UnitList.route,
        modifier = modifier
    ) {
        // 1. Table of Contents / Unit List
        composable(Screen.UnitList.route) {
            UnitListScreen(
                audioPlayerManager = audioPlayerManager,
                onUnitClick = { unitNum ->
                    navController.navigate(Screen.UnitDetail.createRoute(unitNum))
                },
                onOpenVocabulary = {
                    navController.navigate(Screen.Vocabulary.createRoute(null))
                },
                onOpenVerbs = {
                    navController.navigate(Screen.Verbs.createRoute(null))
                },
                onOpenCollocations = {
                    navController.navigate(Screen.Collocations.createRoute(null))
                }
            )
        }

        // 2. Unit Detail (Reading, Paragraphs, Karaoke, Quran Recitation)
        composable(
            route = Screen.UnitDetail.route,
            arguments = listOf(
                navArgument("unitNum") {
                    type = NavType.IntType
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val unitNum = backStackEntry.arguments?.getInt("unitNum") ?: 1
            UnitDetailScreen(
                unitNum = unitNum,
                audioPlayerManager = audioPlayerManager,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToVocabulary = { uNum ->
                    navController.navigate(Screen.Vocabulary.createRoute(uNum))
                },
                onNavigateToVerbs = { uNum ->
                    navController.navigate(Screen.Verbs.createRoute(uNum))
                },
                onNavigateToCollocations = { uNum ->
                    navController.navigate(Screen.Collocations.createRoute(uNum))
                }
            )
        }

        // 3. Searchable Vocabulary Dictionary
        composable(
            route = Screen.Vocabulary.route,
            arguments = listOf(
                navArgument("unitNum") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val unitNumArg = backStackEntry.arguments?.getString("unitNum")?.toIntOrNull()
            VocabularyScreen(
                initialUnitNum = unitNumArg,
                audioPlayerManager = audioPlayerManager,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 4. Searchable Verbs Conjugation Table
        composable(
            route = Screen.Verbs.route,
            arguments = listOf(
                navArgument("unitNum") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val unitNumArg = backStackEntry.arguments?.getString("unitNum")?.toIntOrNull()
            VerbsScreen(
                initialUnitNum = unitNumArg,
                audioPlayerManager = audioPlayerManager,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 5. Searchable Collocations & Idioms Table
        composable(
            route = Screen.Collocations.route,
            arguments = listOf(
                navArgument("unitNum") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val unitNumArg = backStackEntry.arguments?.getString("unitNum")?.toIntOrNull()
            CollocationsScreen(
                initialUnitNum = unitNumArg,
                audioPlayerManager = audioPlayerManager,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
