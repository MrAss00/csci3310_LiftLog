package edu.cuhk.csci3310.liftlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import edu.cuhk.csci3310.liftlog.data.local.dao.ExerciseBest
import edu.cuhk.csci3310.liftlog.data.local.dao.ExerciseSetCount
import edu.cuhk.csci3310.liftlog.formatWeight
import edu.cuhk.csci3310.liftlog.formatWithCommas
import edu.cuhk.csci3310.liftlog.titlecase
import edu.cuhk.csci3310.liftlog.toAverageMinutes
import edu.cuhk.csci3310.liftlog.ui.components.LiftLogTabScaffold
import edu.cuhk.csci3310.liftlog.ui.viewmodel.HeatmapDay
import edu.cuhk.csci3310.liftlog.ui.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(
    navController: NavHostController,
    viewModel: StatsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val (
        monthlySessions,
        monthlyVolume,
        monthlyTotalSets,
        averageDuration,
        heatmapData,
        topExercises,
        personalRecords,
    ) = state

    LiftLogTabScaffold(navController, topBar = {}) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SectionLabel("This Month")
                Spacer(Modifier.height(8.dp))
                MonthlyOverviewRow(
                    sessions = monthlySessions,
                    volume = monthlyVolume,
                    totalSets = monthlyTotalSets,
                )
            }
            item {
                AverageDurationCard(averageDuration)
            }
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Activity Heatmap")
                Spacer(Modifier.height(8.dp))
                ActivityHeatmap(heatmapData)
            }
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Personal Records")
                Spacer(Modifier.height(8.dp))
                PersonalRecordsCard(personalRecords)
            }
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Top Exercises")
                Spacer(Modifier.height(8.dp))
                TopExercisesCard(topExercises)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MonthlyOverviewRow(sessions: Int, volume: Long, totalSets: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatCard(
            icon = Icons.Default.FitnessCenter,
            value = sessions.toString(),
            label = "Sessions",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Default.Repeat,
            value = volume.formatWithCommas(),
            label = "Volume (kg)",
            tint = Color(0xFF2196F3),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Default.Repeat,
            value = totalSets.toString(),
            label = "Total Sets",
            tint = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
private fun AverageDurationCard(average: Long) {
    val displayText = average.toAverageMinutes()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Average Session Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ActivityHeatmap(days: List<HeatmapDay>) {
    val today = remember { LocalDate.now() }

    val colorEmpty = MaterialTheme.colorScheme.surfaceVariant
    val colorFull = MaterialTheme.colorScheme.primary
    // future days: same hue as empty but noticeably dimmer
    val colorFuture = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val onEmpty = MaterialTheme.colorScheme.onSurfaceVariant
    val onFull = MaterialTheme.colorScheme.onPrimary

    val maxVolume = days.maxOfOrNull { it.volume }?.takeIf { it > 0 } ?: 1L

    val daysInMonth = today.lengthOfMonth()
    val todayDayNum = today.dayOfMonth

    // Java DayOfWeek: MONDAY=1 … SUNDAY=7; we want Sun=0 … Sat=6
    val startOffset = today.withDayOfMonth(1).dayOfWeek.value % 7

    val monthLabel = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            " " + today.year

    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // total grid slots needed (leading empties + all days in month), rounded up to full weeks
    val totalSlots = startOffset + daysInMonth
    val totalRows = (totalSlots + 6) / 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (col in 0 until 7) {
                            val slot = row * 7 + col
                            val dayNum = slot - startOffset + 1
                            if (dayNum !in 1..daysInMonth) {
                                // empty spacer —  keeps grid aligned
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                )
                            } else {
                                val isFuture = dayNum > todayDayNum
                                val volume =
                                    if (isFuture) 0L else days.getOrNull(dayNum - 1)?.volume ?: 0L

                                val cellColor = when {
                                    isFuture -> colorFuture
                                    volume == 0L -> colorEmpty
                                    else -> {
                                        val fraction =
                                            (volume.toFloat() / maxVolume.toFloat()).coerceIn(
                                                0f,
                                                1f,
                                            )
                                        lerp(colorEmpty, colorFull, 0.25f + fraction * 0.75f)
                                    }
                                }
                                val textColor = when {
                                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.4f,
                                    )

                                    volume == 0L -> onEmpty
                                    else -> {
                                        val fraction =
                                            (volume.toFloat() / maxVolume.toFloat()).coerceIn(
                                                0f,
                                                1f,
                                            )
                                        lerp(onEmpty, onFull, 0.25f + fraction * 0.75f)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cellColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                listOf(0f, 0.3f, 0.55f, 0.75f, 1f).forEach { fraction ->
                    val c = if (fraction == 0f) colorEmpty
                    else lerp(colorEmpty, colorFull, 0.25f + fraction * 0.75f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordsCard(records: List<ExerciseBest>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (records.isEmpty()) {
                EmptyHint("no exercises logged yet")
            } else {
                records.forEachIndexed { index, pr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = pr.exerciseName.titlecase(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${pr.maxWeight.formatWeight()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                        )
                    }
                    if (index < records.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopExercisesCard(exercises: List<ExerciseSetCount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (exercises.isEmpty()) {
                EmptyHint("no exercises logged yet")
            } else {
                val maxSets = exercises.firstOrNull()?.totalCompletedSets?.takeIf { it > 0 } ?: 1
                exercises.forEachIndexed { index, ex ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = ex.exerciseName.titlecase(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${ex.totalCompletedSets} sets",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val fraction = ex.totalCompletedSets.toFloat() / maxSets.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    if (index < exercises.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
