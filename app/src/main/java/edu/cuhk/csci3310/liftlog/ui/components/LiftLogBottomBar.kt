package edu.cuhk.csci3310.liftlog.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import edu.cuhk.csci3310.liftlog.ui.navigation.Screen

@Composable
fun LiftLogBottomBar(navController: NavHostController) {
    val tabs = listOf(Screen.Stats, Screen.Log, Screen.Routines)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        tabs.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                label = { Text(text = screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // pop up to the start destination to avoid building up a large back stack
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true // avoid multiple copies of the same destination
                        restoreState = true // restore state when re-selecting a previous tab
                    }
                },
            )
        }
    }
}
