package edu.cuhk.csci3310.liftlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWorkout
import edu.cuhk.csci3310.liftlog.titlecase
import edu.cuhk.csci3310.liftlog.ui.viewmodel.RoutineEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditScreen(
    navController: NavHostController,
    viewModel: RoutineEditViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    var showExerciseSearch by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // navigate back on successful save
    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    // show error message
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.editing) "Edit Routine" else "New Routine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            if (viewModel.editing) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                    ) {
                        if (state.saving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        label = { Text("Routine Name") },
                        placeholder = { Text("e.g. Push Day") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Workouts (${state.workouts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                itemsIndexed(
                    state.workouts,
                    key = { index, w -> "${w.exerciseId}_$index" },
                ) { index, workout ->
                    WorkoutItem(
                        workout = workout,
                        index = index,
                        isFirst = index == 0,
                        isLast = index == state.workouts.size - 1,
                        onUpdate = { viewModel.updateWorkout(index, it) },
                        onRemove = { viewModel.removeWorkout(index) },
                        onMoveUp = {
                            if (index > 0) {
                                viewModel.moveWorkout(index, index - 1)
                            }
                        },
                        onMoveDown = {
                            if (index < state.workouts.size - 1) {
                                viewModel.moveWorkout(index, index + 1)
                            }
                        },
                    )
                }
                item {
                    Button(
                        onClick = { showExerciseSearch = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Workout")
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showExerciseSearch) {
        ExerciseSearchDialog(
            onDismiss = { showExerciseSearch = false },
            onExerciseSelected = { exercise ->
                viewModel.addWorkout(
                    RoutineWorkout(
                        routineId = 0,
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        exerciseGifUrl = exercise.gifUrl,
                        sets = 0,
                        reps = 0,
                        weight = 0.0,
                        interval = 0,
                        index = 0,
                    ),
                )
                showExerciseSearch = false
            },
        )
    }
}

@Composable
private fun WorkoutItem(
    workout: RoutineWorkout,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onUpdate: (RoutineWorkout) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(workout.exerciseGifUrl)
                        .decoderFactory(GifDecoder.Factory())
                        .crossfade(true)
                        .build(),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                    contentDescription = workout.exerciseName,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}. ${workout.exerciseName.titlecase()}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(
                        Icons.Filled.ArrowDownward,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberField(
                    label = "Sets",
                    value = workout.sets,
                    onValueChange = { onUpdate(workout.copy(sets = it)) },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "Reps",
                    value = workout.reps,
                    onValueChange = { onUpdate(workout.copy(reps = it)) },
                    modifier = Modifier.weight(1f),
                )
                DecimalField(
                    label = "Weight",
                    value = workout.weight,
                    onValueChange = { onUpdate(workout.copy(weight = it)) },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "Interval",
                    value = workout.interval,
                    onValueChange = { onUpdate(workout.copy(interval = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun DecimalField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(if (value == 0.0) "" else value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toDoubleOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
