package edu.cuhk.csci3310.liftlog.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWorkout
import edu.cuhk.csci3310.liftlog.data.repository.SettingsRepository
import edu.cuhk.csci3310.liftlog.titlecase
import edu.cuhk.csci3310.liftlog.toTimerString
import edu.cuhk.csci3310.liftlog.toVerboseDuration
import edu.cuhk.csci3310.liftlog.ui.speech.SpeechManager
import edu.cuhk.csci3310.liftlog.ui.viewmodel.SpotterViewModel
import org.json.JSONArray

@Composable
fun SpotterScreen(
    navController: NavHostController,
    viewModel: SpotterViewModel = viewModel(),
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsState()

    // read the voice rep-counting toggle from settings
    val settingsRepo = remember { SettingsRepository(context) }
    val speechEnabled by settingsRepo.speechRecognitionEnabled.collectAsState(initial = false)

    var showEndSessionDialog by remember { mutableStateOf(false) }
    var showInstructionsFor by remember { mutableStateOf<String?>(null) }

    // track whether RECORD_AUDIO has been granted
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasAudioPermission = granted }

    // ask for the permission once when the screen first appears, but only if the feature is on
    LaunchedEffect(speechEnabled) {
        if (speechEnabled && !hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // single SpeechManager instance, recreated only if permission/feature-toggle state changes
    val speechManager = remember(hasAudioPermission, speechEnabled) {
        if (hasAudioPermission && speechEnabled) {
            SpeechManager(
                context = context,
                onNumberDetected = { reps ->
                    if (reps > state.currentReps) {
                        viewModel.setCurrentReps(reps)
                    }
                },
                onCompleteSet = viewModel::completeSet,
            )
        } else {
            null
        }
    }

    DisposableEffect(speechManager) {
        onDispose { speechManager?.release() }
    }

    // start or stop the microphone whenever the timer state or feature toggle changes
    LaunchedEffect(
        state.isTimerRunning,
        state.isCompleted,
        hasAudioPermission,
        speechEnabled,
        speechManager,
    ) {
        if (speechManager == null || state.isCompleted) {
            speechManager?.stopListening()
            return@LaunchedEffect
        }
        if (state.isTimerRunning) {
            speechManager.stopListening()
        } else {
            speechManager.startListening()
        }
    }

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
                            speechEnabled = speechEnabled,
                            currentSet = state.currentSet,
                            currentReps = state.currentReps,
                            workoutNumber = state.currentWorkoutIndex + 1,
                            totalWorkouts = state.totalWorkouts,
                            onCompleteSet = viewModel::completeSet,
                            onCompleteWorkout = viewModel::completeWorkout,
                            onEndSession = { showEndSessionDialog = true },
                            onShowInstructions = { showInstructionsFor = workout.exerciseId },
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

    showInstructionsFor?.let { exerciseId ->
        val instructions = remember(exerciseId) {
            loadExerciseInstructions(context, exerciseId)
        }
        ExerciseInstructionsDialog(
            instructions = instructions,
            onDismiss = { showInstructionsFor = null },
        )
    }
}

@Composable
private fun WorkoutView(
    workout: RoutineWorkout,
    speechEnabled: Boolean,
    currentSet: Int,
    currentReps: Int,
    workoutNumber: Int,
    totalWorkouts: Int,
    onCompleteSet: () -> Unit,
    onCompleteWorkout: () -> Unit,
    onEndSession: () -> Unit,
    onShowInstructions: () -> Unit,
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
            if (speechEnabled) {
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    targetState = currentReps,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "rep_counter",
                ) { reps ->
                    Text(
                        text = if (reps == 0) "Listening for reps…" else "Rep $reps / ${workout.reps}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (reps >= workout.reps)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (reps >= workout.reps) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onShowInstructions,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "View Instructions",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
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
            text = countdown.toTimerString(),
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

@Composable
private fun SessionCompletedView(
    startTime: Long,
    onDone: () -> Unit,
) {
    val duration = System.currentTimeMillis() - startTime
    val formattedDuration = duration.toVerboseDuration()

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

private fun formatWeight(weight: Double): String {
    return if (weight == weight.toLong().toDouble()) {
        "${weight.toLong()} kg"
    } else {
        "$weight kg"
    }
}

private fun loadExerciseInstructions(
    context: android.content.Context,
    exerciseId: String,
): List<String> {
    return try {
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString("exerciseId") == exerciseId) {
                val instructionsArray = obj.getJSONArray("instructions")
                return (0 until instructionsArray.length()).map { instructionsArray.getString(it) }
            }
        }
        emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
private fun ExerciseInstructionsDialog(
    instructions: List<String>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instructions") },
        text = {
            if (instructions.isEmpty()) {
                Text("no instructions available")
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    instructions.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
