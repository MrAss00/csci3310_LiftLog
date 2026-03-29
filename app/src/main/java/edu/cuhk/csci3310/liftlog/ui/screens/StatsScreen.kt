package edu.cuhk.csci3310.liftlog.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import edu.cuhk.csci3310.liftlog.ui.components.LiftLogTabScaffold

@Composable
fun StatsScreen(navController: NavHostController) {
    LiftLogTabScaffold(navController, title = "Stats") { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Stats",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
