package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionExerciseEntity
import edu.cuhk.csci3310.liftlog.data.local.model.Routine
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWorkout
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import edu.cuhk.csci3310.liftlog.data.repository.SessionRepository
import edu.cuhk.csci3310.liftlog.service.RestTimerService
import edu.cuhk.csci3310.liftlog.service.RestTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpotterViewState(
    val routine: Routine? = null,
    val currentWorkoutIndex: Int = 0,
    val currentSet: Int = 1,
    val currentReps: Int = 0,
    val isTimerRunning: Boolean = false,
    val countdown: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isSaved: Boolean = false,
) {
    val currentWorkout: RoutineWorkout?
        get() = routine?.workouts?.sortedBy { it.index }?.getOrNull(currentWorkoutIndex)

    val totalWorkouts: Int
        get() = routine?.workouts?.size ?: 0

    val nextWorkout: RoutineWorkout?
        get() = routine?.workouts?.sortedBy { it.index }?.getOrNull(currentWorkoutIndex + 1)

    val isLastWorkout: Boolean
        get() = currentWorkoutIndex >= totalWorkouts - 1

    val isLastSet: Boolean
        get() = currentWorkout?.let { currentSet >= it.sets } ?: true
}

class SpotterViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val routineRepository: RoutineRepository
    private val sessionRepository: SessionRepository

    private val _state = MutableStateFlow(SpotterViewState())
    val state: StateFlow<SpotterViewState> = _state.asStateFlow()

    private val routineId: Long = savedStateHandle.get<Long>("routineId") ?: -1L

    init {
        val database = LiftLogDatabase.getInstance(application)
        routineRepository = RoutineRepository(database.routineDao())
        sessionRepository = SessionRepository(database.sessionDao())

        loadRoutine()
        observeTimerState()
    }

    private fun loadRoutine() {
        viewModelScope.launch {
            val routine = routineRepository.getRoutineById(routineId).firstOrNull()
            _state.update {
                it.copy(
                    routine = routine,
                    startTime = System.currentTimeMillis(),
                )
            }
        }
    }

    /**
     * Collects from [RestTimerState] and mirrors the values into [_state].
     * Also reacts to the one-shot "finished" event to advance workout progress.
     */
    private fun observeTimerState() {
        viewModelScope.launch {
            RestTimerState.timeRemaining.collect { seconds ->
                _state.update { it.copy(countdown = seconds) }
            }
        }
        viewModelScope.launch {
            RestTimerState.isRunning.collect { running ->
                _state.update { it.copy(isTimerRunning = running) }
            }
        }
        viewModelScope.launch {
            RestTimerState.timerFinished.collect {
                onTimerFinished()
            }
        }
    }

    fun setCurrentReps(reps: Int) {
        _state.update { it.copy(currentReps = reps) }
    }

    fun completeSet() {
        val state = _state.value
        val workout = state.currentWorkout ?: return

        if (state.currentSet < workout.sets) {
            // more sets remaining in this workout,
            // start timer then go to next set
            startTimer(workout.interval)
        } else {
            // last set of this workout
            if (state.isLastWorkout) {
                // all workouts completed
                completeSession()
            } else {
                // move to next workout after timer
                startTimer(workout.interval)
            }
        }
    }

    fun completeWorkout() {
        val state = _state.value
        val workout = state.currentWorkout ?: return

        if (state.isLastWorkout) {
            // all workouts completed
            completeSession()
        } else {
            // move to next workout after timer
            _state.update { it.copy(currentSet = workout.sets) } // mark all sets as done
            startTimer(workout.interval)
        }
    }

    private fun startTimer(durationSeconds: Int) {
        val context = getApplication<Application>()
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_DURATION_SECONDS, durationSeconds)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun skipTimer() {
        val context = getApplication<Application>()
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_SKIP
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun onTimerFinished() {
        val state = _state.value
        val workout = state.currentWorkout

        _state.update { it.copy(isTimerRunning = false, countdown = 0, currentReps = 0) }

        if (workout == null) return

        if (state.currentSet < workout.sets) {
            // more sets remaining, go to next set
            _state.update { it.copy(currentSet = it.currentSet + 1) }
        } else {
            // all sets done for this workout, move to next workout
            if (!state.isLastWorkout) {
                _state.update {
                    it.copy(
                        currentWorkoutIndex = it.currentWorkoutIndex + 1,
                        currentSet = 1,
                    )
                }
            } else {
                // session complete
                completeSession()
            }
        }
    }

    private fun completeSession() {
        _state.update { it.copy(isCompleted = true) }
        saveSession()
    }

    fun endSessionEarly() {
        saveSession()
    }

    private fun saveSession() {
        if (_state.value.isSaved) return

        viewModelScope.launch {
            val routine = _state.value.routine ?: return@launch

            var totalVolume = 0L
            var totalSets = 0

            val currentWorkoutIndex = _state.value.currentWorkoutIndex
            val currentSetInLastWorkout = _state.value.currentSet
            val isSessionFullyCompleted = _state.value.isCompleted

            val setsCompletedPerWorkout = routine.workouts
                .sortedBy { it.index }
                .mapIndexed { index, workout ->
                    val setsCompleted = when {
                        index < currentWorkoutIndex -> workout.sets
                        index == currentWorkoutIndex -> {
                            if (isSessionFullyCompleted) {
                                workout.sets
                            } else {
                                (currentSetInLastWorkout - 1).coerceAtLeast(0)
                            }
                        }

                        else -> 0
                    }
                    workout to setsCompleted
                }

            setsCompletedPerWorkout.forEach { (workout, setsCompleted) ->
                totalSets += setsCompleted
                totalVolume += (workout.weight * workout.reps * setsCompleted).toLong()
            }


            val session = SessionEntity(
                routineId = routine.routine.id,
                routineName = routine.routine.name,
                startTime = _state.value.startTime,
                endTime = System.currentTimeMillis(),
                totalVolume = totalVolume,
                totalSets = totalSets,
            )
            val sessionId = sessionRepository.insertSession(session)

            // Save ALL planned exercises; completedSets tracks actual progress per exercise
            val sessionExercises = setsCompletedPerWorkout
                .mapIndexed { index, (workout, setsCompleted) ->
                    SessionExerciseEntity(
                        sessionId = sessionId,
                        exerciseName = workout.exerciseName,
                        sets = workout.sets,
                        completedSets = setsCompleted,
                        reps = workout.reps,
                        weight = workout.weight,
                        index = index,
                    )
                }
            sessionRepository.insertSessionExercises(sessionExercises)

            _state.update { it.copy(isSaved = true) }
            requestWidgetUpdate()
        }
    }

    private fun requestWidgetUpdate() {
        val context = getApplication<Application>()
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(
            context,
            edu.cuhk.csci3310.liftlog.widget.LiftLogWidgetReceiver::class.java,
        )
        val ids = manager.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                setComponent(component)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Stop the foreground service when the ViewModel is destroyed (session ended or
        // app process killed). This ensures no stale notification lingers.
        val context = getApplication<Application>()
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP
        }
        context.startService(intent)
    }
}
