package edu.cuhk.csci3310.liftlog.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import edu.cuhk.csci3310.liftlog.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogTopBar(title: String = "LiftLog", navController: NavHostController) {
    TopAppBar(
        title = { Text(text = title) },
        actions = {
            IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                )
            }
        },
    )
}
