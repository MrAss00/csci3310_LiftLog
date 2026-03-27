package edu.cuhk.csci3310.liftlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import edu.cuhk.csci3310.liftlog.ui.components.LiftLogBottomBar
import edu.cuhk.csci3310.liftlog.ui.components.LiftLogTopBar
import edu.cuhk.csci3310.liftlog.ui.navigation.Screen
import edu.cuhk.csci3310.liftlog.ui.screens.LogScreen
import edu.cuhk.csci3310.liftlog.ui.screens.RoutinesScreen
import edu.cuhk.csci3310.liftlog.ui.screens.SpotterScreen
import edu.cuhk.csci3310.liftlog.ui.screens.StatsScreen
import edu.cuhk.csci3310.liftlog.ui.theme.LiftLogTheme

class MainActivity : ComponentActivity() {
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

@Composable
fun LiftLogApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LiftLogTopBar(navController) },
        bottomBar = { LiftLogBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Stats.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Routines.route) { RoutinesScreen() }
            composable(Screen.Spotter.route) { SpotterScreen() }
            composable(Screen.Log.route) { LogScreen() }
        }
    }
}
