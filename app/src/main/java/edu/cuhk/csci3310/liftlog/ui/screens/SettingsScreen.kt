package edu.cuhk.csci3310.liftlog.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import edu.cuhk.csci3310.liftlog.ui.components.LiftLogTabScaffold
import edu.cuhk.csci3310.liftlog.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel(),
) {
    val speechEnabled by viewModel.speechRecognitionEnabled.collectAsState()
    val useRemoteExercises by viewModel.useRemoteExercises.collectAsState()

    LiftLogTabScaffold(title = "Settings", navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Spotter section ───────────────────────────────────────────────
            Text(
                text = "Spotter",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            SettingsToggleRow(
                title = "Voice Rep Counting",
                description = "Automatically count reps from your voice during a set.",
                checked = speechEnabled,
                onCheckedChange = viewModel::setSpeechRecognitionEnabled,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Exercises section ─────────────────────────────────────────────
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            SettingsToggleRow(
                title = "Online Exercise Database",
                description = "Fetch exercises from the remote API instead of the bundled local library. Requires an internet connection.",
                checked = useRemoteExercises,
                onCheckedChange = viewModel::setUseRemoteExercises,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
