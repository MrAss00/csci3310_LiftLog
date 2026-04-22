package edu.cuhk.csci3310.liftlog.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogTopBar(title: String = "LiftLog", navController: NavHostController) {
    TopAppBar(
        title = { Text(text = title) },
    )
}
