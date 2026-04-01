package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.entity.SessionEntity
import edu.cuhk.csci3310.liftlog.data.local.model.Routine
import edu.cuhk.csci3310.liftlog.data.local.model.RoutineWorkout
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import edu.cuhk.csci3310.liftlog.data.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var timer: Job? = null

    init {
        val database = LiftLogDatabase.getInstance(application)
        routineRepository = RoutineRepository(database.routineDao())
        sessionRepository = SessionRepository(database.sessionDao())

        loadRoutine()
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
        timer?.cancel()
        _state.update {
            it.copy(
                isTimerRunning = true,
                countdown = durationSeconds,
            )
        }
        timer = viewModelScope.launch {
            while (_state.value.countdown > 0) {
                delay(1000)
                _state.update { it.copy(countdown = it.countdown - 1) }
            }
            onTimerFinished()
        }
    }

    fun skipTimer() {
        timer?.cancel()
        onTimerFinished()
    }

    private fun onTimerFinished() {
        val state = _state.value
        val workout = state.currentWorkout

        _state.update { it.copy(isTimerRunning = false, countdown = 0) }

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

            // for calculate the totalvolume done in a session
            var totalVolume = 0L
            var totalSets = 0

            // parse total volume
            val currentWorkoutIndex = _state.value.currentWorkoutIndex
            val currentSetInLastWorkout = _state.value.currentSet
            val isSessionFullyCompleted = _state.value.isCompleted

            routine.workouts.forEachIndexed { index, workout ->
                val setsCompletedInThisWorkout = when {
                    index < currentWorkoutIndex -> workout.sets
                    //current
                    index == currentWorkoutIndex -> {
                        if (isSessionFullyCompleted){
                            workout.sets
                        }
                        else{
                            (currentSetInLastWorkout - 1).coerceAtLeast(0)
                        }
                    }
                    else -> 0
                }

                totalSets += setsCompletedInThisWorkout
                totalVolume += (workout.weight * workout.reps * setsCompletedInThisWorkout).toLong()
            }


            val session = SessionEntity(
                routineId = routine.routine.id,
                routineName = routine.routine.name,
                startTime = _state.value.startTime,
                endTime = System.currentTimeMillis(),
                totalVolume = totalVolume,
                setsCount = totalSets
            )
            sessionRepository.insertSession(session)
            _state.update { it.copy(isSaved = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
