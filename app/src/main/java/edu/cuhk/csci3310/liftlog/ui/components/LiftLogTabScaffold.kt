package edu.cuhk.csci3310.liftlog.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun LiftLogTabScaffold(
    navController: NavHostController,
    title: String,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { LiftLogTopBar(title) },
        bottomBar = { LiftLogBottomBar(navController) }
    ) { innerPadding ->
        content(innerPadding)
    }
}
