package edu.cuhk.csci3310.liftlog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import edu.cuhk.csci3310.liftlog.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val monthlyGoal by viewModel.monthlyGoal.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()

    var monthlyInput by remember { mutableStateOf(monthlyGoal.toString()) }
    var dailyInput by remember { mutableStateOf(dailyGoal.toString()) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Monthly Goal
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Goal (kg)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = monthlyInput,
                        onValueChange = { monthlyInput = it },
                        label = { Text("Monthly goal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.updateMonthlyGoal(monthlyInput.toLongOrNull() ?: 100000L) }) {
                        Text("Save Monthly Goal")
                    }
                }
            }

            // Daily Goal for Today
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Goal for Today (kg)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = dailyInput,
                        onValueChange = { dailyInput = it },
                        label = { Text("Daily goal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.updateDailyGoal(dailyInput.toLongOrNull() ?: 2000L) }) {
                        Text("Save Daily Goal")
                    }
                }
            }
        }
    }
}