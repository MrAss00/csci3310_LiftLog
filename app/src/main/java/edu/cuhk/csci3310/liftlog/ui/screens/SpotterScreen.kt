package edu.cuhk.csci3310.liftlog.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWorkout
import edu.cuhk.csci3310.liftlog.titlecase
import edu.cuhk.csci3310.liftlog.ui.viewmodel.SpotterViewModel
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun SpotterScreen(
    navController: NavHostController,
    viewModel: SpotterViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showEndSessionDialog by remember { mutableStateOf(false) }

    // navigate back only when session is ended early
    LaunchedEffect(state.isSaved, state.isCompleted) {
        if (state.isSaved && !state.isCompleted) {
            navController.popBackStack()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.routine == null -> {
                    Text(
                        text = "routine not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.isCompleted -> {
                    SessionCompletedView(
                        startTime = state.startTime,
                        onDone = { navController.popBackStack() },
                    )
                }

                state.isTimerRunning -> {
                    TimerView(
                        countdown = state.countdown,
                        currentWorkout = state.currentWorkout,
                        nextWorkout = if (state.isLastSet) state.nextWorkout else null,
                        currentSet = state.currentSet,
                        totalSets = state.currentWorkout?.sets ?: 0,
                        onSkip = viewModel::skipTimer,
                    )
                }

                else -> {
                    state.currentWorkout?.let { workout ->
                        WorkoutView(
                            workout = workout,
                            currentSet = state.currentSet,
                            workoutNumber = state.currentWorkoutIndex + 1,
                            totalWorkouts = state.totalWorkouts,
                            onCompleteSet = viewModel::completeSet,
                            onCompleteWorkout = viewModel::completeWorkout,
                            onEndSession = { showEndSessionDialog = true },
                        )
                    }
                }
            }
        }
    }

    if (showEndSessionDialog) {
        EndSessionDialog(
            currentWorkoutIndex = state.currentWorkoutIndex,
            totalWorkouts = state.totalWorkouts,
            onConfirm = {
                showEndSessionDialog = false
                viewModel.endSessionEarly()
            },
            onDismiss = { showEndSessionDialog = false },
        )
    }
}

@Composable
private fun WorkoutView(
    workout: RoutineWorkout,
    currentSet: Int,
    workoutNumber: Int,
    totalWorkouts: Int,
    onCompleteSet: () -> Unit,
    onCompleteWorkout: () -> Unit,
    onEndSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(workout.exerciseGifUrl)
                    .decoderFactory(GifDecoder.Factory())
                    .crossfade(true)
                    .build(),
                modifier = Modifier
                    .size(200.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
                contentDescription = workout.exerciseName,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = workout.exerciseName.titlecase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Workout $workoutNumber of $totalWorkouts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = currentSet,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "set_counter",
            ) { set ->
                Text(
                    text = "Set $set of ${workout.sets}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "${workout.reps} reps  •  ${formatWeight(workout.weight)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onCompleteSet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = "Complete This Set",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            OutlinedButton(
                onClick = onCompleteWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = "Complete This Workout",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(
                onClick = onEndSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(
                    text = "End Session",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun TimerView(
    countdown: Int,
    currentWorkout: RoutineWorkout?,
    nextWorkout: RoutineWorkout?,
    currentSet: Int,
    totalSets: Int,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "REST",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = formatTime(countdown),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Next Up",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        if (nextWorkout != null) {
            Text(
                text = nextWorkout.exerciseName.titlecase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else if (currentWorkout != null && currentSet < totalSets) {
            Text(
                text = currentWorkout.exerciseName.titlecase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Set ${currentSet + 1} of $totalSets",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(48.dp))
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(48.dp),
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun SessionCompletedView(
    startTime: Long,
    onDone: () -> Unit,
) {
    val duration = System.currentTimeMillis() - startTime
    val formattedDuration =
        duration.toDuration(DurationUnit.MILLISECONDS).toComponents { minues, seconds, _ ->
            String.format("%02d minutes %02d seconds", minues, seconds)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Session Complete",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = formattedDuration,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(48.dp),
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun EndSessionDialog(
    currentWorkoutIndex: Int,
    totalWorkouts: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Session?") },
        text = {
            Text(
                if (currentWorkoutIndex == 0) {
                    "You haven't completed any workouts yet. End the session now?"
                } else {
                    "You have completed $currentWorkoutIndex of $totalWorkouts workouts. End the session now?"
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("End Session", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "%d:%02d".format(minutes, secs)
    } else {
        "0:%02d".format(secs)
    }
}

private fun formatWeight(weight: Double): String {
    return if (weight == weight.toLong().toDouble()) {
        "${weight.toLong()} kg"
    } else {
        "$weight kg"
    }
}
