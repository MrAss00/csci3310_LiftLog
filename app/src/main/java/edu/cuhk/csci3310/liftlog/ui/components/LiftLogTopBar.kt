package edu.cuhk.csci3310.liftlog.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import edu.cuhk.csci3310.liftlog.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogTopBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val title = Screen.tabs
        .firstOrNull { it.route == currentRoute }
        ?.label
        ?: "LiftLog"

    TopAppBar(
        title = { Text(text = title) },
        actions = {
            IconButton(onClick = { /* TODO: open settings */ }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}
