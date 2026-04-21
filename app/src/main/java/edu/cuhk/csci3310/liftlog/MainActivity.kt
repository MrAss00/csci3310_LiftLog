package edu.cuhk.csci3310.liftlog

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import edu.cuhk.csci3310.liftlog.service.RestTimerService
import edu.cuhk.csci3310.liftlog.ui.navigation.Screen
import edu.cuhk.csci3310.liftlog.ui.screens.LogScreen
import edu.cuhk.csci3310.liftlog.ui.screens.RoutineEditScreen
import edu.cuhk.csci3310.liftlog.ui.screens.RoutinesScreen
import edu.cuhk.csci3310.liftlog.ui.screens.SettingsScreen
import edu.cuhk.csci3310.liftlog.ui.screens.SpotterScreen
import edu.cuhk.csci3310.liftlog.ui.screens.StatsScreen
import edu.cuhk.csci3310.liftlog.ui.theme.LiftLogTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createRestTimerNotificationChannel()
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            LiftLogTheme {
                LiftLogApp()
            }
        }
    }

    /** Creates the notification channel used by [RestTimerService]. */
    private fun createRestTimerNotificationChannel() {
        val channel = NotificationChannel(
            RestTimerService.NOTIFICATION_CHANNEL_ID,
            "Rest Timer",
            NotificationManager.IMPORTANCE_LOW,  // silent; no sound/vibration
        ).apply { description = "Shows the rest period countdown during a workout session." }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** On Android 13+, POST_NOTIFICATIONS requires a runtime grant. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun LiftLogApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Stats.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable(Screen.Stats.route) { StatsScreen(navController) }
        composable(
            route = "${Screen.Log.route}?openPicker={openPicker}",
            arguments = listOf(
                navArgument("openPicker") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "liftlog://log?openPicker={openPicker}" },
            ),
        ) { backStackEntry ->
            val openPicker = backStackEntry.arguments?.getBoolean("openPicker") ?: false
            LogScreen(navController, openPicker = openPicker)
        }
        composable(Screen.Routines.route) { RoutinesScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }

        composable(
            "routine_create",
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            },
        ) {
            RoutineEditScreen(navController)
        }
        composable(
            "routine_edit/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
        ) {
            RoutineEditScreen(navController)
        }
        composable(
            "spotter/{routineId}",
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType },
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            },
        ) {
            SpotterScreen(navController)
        }
    }
}
