package edu.cuhk.csci3310.liftlog

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.cuhk.csci3310.liftlog.ui.navigation.Screen
import edu.cuhk.csci3310.liftlog.ui.screens.LogScreen
import edu.cuhk.csci3310.liftlog.ui.screens.RoutineEditScreen
import edu.cuhk.csci3310.liftlog.ui.screens.RoutinesScreen
import edu.cuhk.csci3310.liftlog.ui.screens.SettingsScreen
import edu.cuhk.csci3310.liftlog.ui.screens.StatsScreen
import edu.cuhk.csci3310.liftlog.ui.theme.LiftLogTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiftLogTheme {
                LiftLogApp()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LiftLogApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Stats.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(Screen.Stats.route) { StatsScreen(navController) }
        composable(Screen.Log.route) { LogScreen(navController) }
        composable(Screen.Routines.route) { RoutinesScreen(navController) }
        composable(Screen.Settings.route) {SettingsScreen(navController) }

        composable(
            "routine_create",
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            }
        ) {
            RoutineEditScreen(navController)
        }
        composable(
            "routine_edit/{routineId}",
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType },
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        ) {
            RoutineEditScreen(navController)
        }
    }
}
