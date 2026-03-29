package edu.cuhk.csci3310.liftlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Stats : Screen(
        route = "stats",
        label = "Stats",
        icon = Icons.Filled.BarChart
    )

    data object Log : Screen(
        route = "log",
        label = "Log",
        icon = Icons.AutoMirrored.Filled.MenuBook
    )

    data object Routines : Screen(
        route = "routines",
        label = "Routines",
        icon = Icons.Filled.FitnessCenter
    )

    object Settings : Screen(
        route = "settings",
        label = "Settings",
        icon = Icons.Default.Settings

    )
}
