package edu.cuhk.csci3310.liftlog.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import edu.cuhk.csci3310.liftlog.ui.components.LiftLogTabScaffold
import edu.cuhk.csci3310.liftlog.ui.viewmodel.StatsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatsScreen(
    navController: NavHostController,
    viewModel: StatsViewModel = viewModel(),
) {

    val monthlyVolume by viewModel.monthlyVolume.collectAsState(initial = 20L)
    val monthlyGoal by viewModel.monthlyGoal.collectAsState(initial = 100000L)
    val monthlyProgress by viewModel.monthlyProgress.collectAsState(initial = 20f)
    val monthlySessions by viewModel.monthlySessions.collectAsState(initial = 0)
    val monthlyTotalSets by viewModel.monthlyTotalSets.collectAsState(initial = 0)
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val todayVolume by viewModel.todayVolume.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshMonthlyGoal()
        viewModel.refreshDailyGoal() // both functions force stat screen to read the variavles again
        viewModel.refreshTodayVolume()
    }

    LiftLogTabScaffold(navController, title = "Summary") { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { MonthlyGoalCard(monthlyVolume, monthlyGoal, monthlyProgress) }
            item { DailyStatsRow(monthlySessions,monthlyTotalSets) }   // still hardcoded for now
            item { TodayGoalCards(dailyGoal = dailyGoal, todayVolume) }
            item { WeeklyGoalsSection() }
        }
    }
}

// update monthlygoal
@Composable
private fun MonthlyGoalCard(volume: Long, goal: Long, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Monthly goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${volume.formatWithCommas()} / ${goal.formatWithCommas()} kg",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DailyStatsRow(sessions: Int, totalSets: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatSmallCard(
            icon = Icons.Default.FitnessCenter,
            value = sessions.toString(),
            label = "Sessions",
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f),
        )
        StatSmallCard(
            icon = Icons.Default.Repeat,
            value = totalSets.toString(),
            label = "Total Sets",
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f),
        )
        StatSmallCard(
            icon = Icons.Default.ArrowUpward,
            value = "12",
            label = "PRs",
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatSmallCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayGoalCards(dailyGoal:Long,TodayVolume : Long =0) {
    val remaining = (dailyGoal - TodayVolume).coerceAtLeast(0L)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Today's goal", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${dailyGoal.formatWithCommas()} kg",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Card(
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Remaining", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${remaining.formatWithCommas()} kg",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WeeklyGoalsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Weekly goals",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Check reports →",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your activity for last 7 days",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))

            var completedCount = 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val days = listOf("S", "M", "T", "W", "T", "F", "S")

                val progressValues = listOf(
                    1.0f,
                    1.0f,
                    0.8f,
                    0.5f,
                    1.0f,
                    0.3f,
                    0.0f
                )
                completedCount = progressValues.count { it >= 1.0f }
                days.forEachIndexed { index, day ->
                    val progress = progressValues[index]

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(36.dp),
                                color = if (progress >= 1f) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            // Show checkmark only when fully completed
                            if (progress >= 1f) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${completedCount}/7 Completed",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


private fun Long.formatWithCommas(): String = "%,d".format(this)
