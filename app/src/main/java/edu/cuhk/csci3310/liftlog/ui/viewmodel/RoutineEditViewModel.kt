package edu.cuhk.csci3310.liftlog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import edu.cuhk.csci3310.liftlog.data.local.LiftLogDatabase
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineEntity
import edu.cuhk.csci3310.liftlog.data.local.entity.RoutineWorkoutEntity
import edu.cuhk.csci3310.liftlog.data.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineEditState(
    val name: String = "",
    val workouts: List<RoutineWorkoutEntity> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null
)

class RoutineEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository: RoutineRepository

    private val routineId: Long = savedStateHandle.get<Long>("routineId") ?: -1L
    val editing: Boolean = routineId > 0

    private val _state = MutableStateFlow(RoutineEditState())
    val state: StateFlow<RoutineEditState> = _state.asStateFlow()

    init {
        val dao = LiftLogDatabase.getInstance(application).routineDao()
        repository = RoutineRepository(dao)

        if (editing) loadRoutine()
    }

    private fun loadRoutine() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val routineWithWorkouts = repository.getRoutineById(routineId).firstOrNull()
            if (routineWithWorkouts != null) {
                _state.update {
                    it.copy(
                        name = routineWithWorkouts.routine.name,
                        workouts = routineWithWorkouts.workouts.sortedBy { w -> w.index },
                        loading = false
                    )
                }
            } else {
                _state.update { it.copy(loading = false, message = "routine not found") }
            }
        }
    }

    fun setRoutineName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun addWorkout(workout: RoutineWorkoutEntity) {
        _state.update { it.copy(workouts = it.workouts + workout) }
    }

    fun removeWorkout(index: Int) {
        _state.update { state ->
            state.copy(workouts = state.workouts.toMutableList().apply { removeAt(index) })
        }
    }

    fun updateWorkout(index: Int, workout: RoutineWorkoutEntity) {
        _state.update { state ->
            val updated = state.workouts.toMutableList()
            updated[index] = workout
            state.copy(workouts = updated)
        }
    }

    fun moveWorkout(fromIndex: Int, toIndex: Int) {
        _state.update { state ->
            val list = state.workouts.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(workouts = list)
        }
    }

    fun save() {
        val state = _state.value
        if (state.name.isBlank()) {
            _state.update { it.copy(message = "routine name cannot be empty") }
            return
        }
        if (state.workouts.isEmpty()) {
            _state.update { it.copy(message = "at least one workout required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null) }
            try {
                val now = System.currentTimeMillis()
                val savedRoutineId = if (editing) {
                    repository.updateRoutine(
                        RoutineEntity(
                            id = routineId,
                            name = state.name,
                            updatedAt = now
                        )
                    )
                    routineId
                } else {
                    repository.insertRoutine(
                        RoutineEntity(name = state.name, createdAt = now, updatedAt = now)
                    )
                }

                val workoutEntities = state.workouts.mapIndexed { idx, w ->
                    w.copy(id = 0, routineId = savedRoutineId, index = idx)
                }
                repository.saveWorkoutsForRoutine(savedRoutineId, workoutEntities)

                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, message = e.message ?: "failed to save")
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
